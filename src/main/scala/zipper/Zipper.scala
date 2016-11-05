package zipper

import scala.annotation.{implicitNotFound, tailrec}

/**
 * A typeclass that defines how a certain data structure can be unzipped and zipped back.
 *
 * An instance of Unzip can be automatically derived for a case class C with a single field
 * of type List[C]. In a similar situation, but with a different collection class used, say, Vector,
 * an instance can still be derived like so:
 * {{{
 *   implicit val instance = Unzip.For[C, Vector].derive
 * }}}
 */
@implicitNotFound("Count not find a way to unzip ${A}.")
trait Unzip[A] {
  def unzip(node: A): List[A]
  def zip(node: A, children: List[A]): A
}

object Unzip extends GenericUnzipInstances

/**
 * A Zipper allows to move around a recursive immutable data structure and perform updates.
 *
 * Example:
 * {{{
 *   case class Tree(x: Int, c: List[Tree] = List.empty)
 *
 *   val before = Tree(1, List(Tree(2)))
 *   val after = Tree(1, List(Tree(2), Tree(3)))
 *
 *   Zipper(before).moveDownRight.insertRight(Tree(3, Nil)).commit shouldEqual after
 * }}}
 *
 * See https://www.st.cs.uni-saarland.de/edu/seminare/2005/advanced-fp/docs/huet-zipper.pdf.
 */
case class Zipper[A](
  left: List[A],
  focus: A,
  right: List[A],
  top: Option[Zipper[A]]
)(implicit val unzip: Unzip[A]) {
  import Zipper._

  def stay: MoveResult[A] = MoveResult.Success(this)
  def fail: MoveResult[A] = MoveResult.Failure(this)

  // Sideways

  /** Move left */
  def tryMoveLeft = left match {
    case head :: tail ⇒
      MoveResult.Success(copy(left = tail, focus = head, right = focus :: right))
    case Nil ⇒ fail
  }

  /** Move left or throw if impossible */
  def moveLeft = tryMoveLeft.get

  /** Move right */
  def tryMoveRight = right match {
    case head :: tail ⇒
      MoveResult.Success(copy(right = tail, focus = head, left = focus :: left))
    case Nil ⇒ fail
  }

  /** Move right or throw if impossible */
  def moveRight = tryMoveRight.get

  /** Move to the leftmost position */
  def rewindLeft = cycle(_.tryMoveLeft)

  /** Move left by n */
  def tryMoveLeftBy(n: Int) = tryRepeat(n, _.tryMoveLeft)

  /** Move left by n or throw if impossible */
  def moveLeftBy(n: Int) = tryMoveLeftBy(n).get

  /** Move to the rightmost position */
  def rewindRight = cycle(_.tryMoveRight)

  /** Move right by n */
  def tryMoveRightBy(n: Int) = tryRepeat(n, _.tryMoveRight)

  /** Move right by n or throw if impossible */
  def moveRightBy(n: Int) = tryMoveRightBy(n).get

  // Unzip-zip

  /** Unzip the current node and focus on the left child */
  def tryMoveDownLeft = unzip.unzip(focus) match {
    case head :: tail ⇒
      MoveResult.Success(copy(left = Nil, focus = head, right = tail, top = Some(this)))
    case Nil ⇒ fail
  }

  /** Unzip the current node and focus on the left child, or throw if impossible */
  def moveDownLeft = tryMoveDownLeft.get

  /** Unzip the current node and focus on the right child */
  def tryMoveDownRight = tryMoveDownLeft.map(_.rewindRight)

  /** Unzip the current node and focus on the right child, or throw if impossible */
  def moveDownRight = tryMoveDownRight.get

  /** Unzip the current node and focus on the nth child */
  def tryMoveDownAt(index: Int) = tryMoveDownLeft.map(_.moveRightBy(index))

  /** Unzip the current node and focus on the nth child, or throw if impossible */
  def moveDownAt(index: Int) = tryMoveDownAt(index).get

  /** Zip the current layer and move up */
  def tryMoveUp = top.fold(fail) { z ⇒
    MoveResult.Success {
      z.copy(focus = {
        val children = (focus :: left) reverse_::: right
        unzip.zip(z.focus, children)
      })
    }
  }

  /** Zip the current layer and move up, or throw if impossible */
  def moveUp = tryMoveUp.get

  /** Zip to the top and return the resulting value */
  def commit = cycle(_.tryMoveUp).focus

  // Updates and focus manipulation

  /** Perform a side-effect on the focus */
  def tapFocus(f: A ⇒ Unit) = { f(focus); this }

  /** Replace the focus with a different value */
  def set(value: A) = copy(focus = value)

  /** Update the focus by applying a function */
  def update(f: A ⇒ A) = copy(focus = f(focus))

  /** Insert a new value to the left of focus */
  def insertLeft(value: A) = copy(left = value :: left)

  /** Insert a new value to the right of focus */
  def insertRight(value: A) = copy(right = value :: right)

  /** Move down left and insert a list of values on the left, focusing on the first one */
  def tryInsertDownLeft(values: List[A]) = {
    values ::: unzip.unzip(focus) match {
      case head :: tail ⇒
        Zipper.MoveResult.Success(copy(left = Nil, focus = head, right = tail, top = Some(this)))
      case _ ⇒ fail
    }
  }

  /** Move down left and insert a list of values on the left, focusing on the first one */
  def insertDownLeft(values: List[A]) = tryInsertDownLeft(values).get

  /** Move down right and insert a list of values on the right, focusing on the last one */
  def tryInsertDownRight(values: List[A]) = {
    unzip.unzip(focus) ::: values match {
      case head :: tail ⇒
        Zipper.MoveResult.Success(copy(left = Nil, focus = head, right = tail, top = Some(this)).rewindRight)
      case _ ⇒ fail
    }
  }

  /** Move down right and insert a list of values on the right, focusing on the last one */
  def insertDownRight(values: List[A]) = tryInsertDownRight(values).get

  /** Delete the value in focus and move left */
  def tryDeleteAndMoveLeft = left match {
    case head :: tail ⇒ MoveResult.Success(copy(left = tail, focus = head))
    case Nil ⇒ fail
  }

  /** Delete the value in focus and move left, or throw if impossible */
  def deleteAndMoveLeft = tryDeleteAndMoveLeft.get

  /** Delete the value in focus and move right */
  def tryDeleteAndMoveRight = right match {
    case head :: tail ⇒ MoveResult.Success(copy(right = tail, focus = head))
    case Nil ⇒ fail
  }

  /** Delete the value in focus and move right, or throw if impossible */
  def deleteAndMoveRight = tryDeleteAndMoveRight.get

  /** Delete the value in focus and move up */
  def tryDeleteAndMoveUp = top.fold(fail) { z ⇒
    MoveResult.Success {
      z.copy(focus = {
        val children = left reverse_::: right
        unzip.zip(z.focus, children)
      })
    }
  }

  /** Delete the value in focus and move up, or throw if impossible */
  def deleteAndMoveUp = tryDeleteAndMoveUp.get

  // Loops

  /** Cycle through the moves until a failure is produced and return the last success */
  def cycle(move0: Move[A], moves: Move[A]*): Zipper[A] = {
    val moveList = move0 :: moves.toList
    @tailrec def inner(s: List[Move[A]], acc: Zipper[A]): Zipper[A] =
      s.head(acc) match {
        case MoveResult.Success(z) ⇒ inner(if (s.tail.isEmpty) moveList else s.tail, z)
        case _ ⇒ acc
      }
    inner(moveList, this)
  }

  /** Repeat a move `n` times */
  @tailrec final def tryRepeat(n: Int, move: Move[A]): Zipper.MoveResult[A] =
    if (n < 1) stay else move(this) match {
      case MoveResult.Success(z) ⇒ z.tryRepeat(n - 1, move)
      case failure ⇒ failure
    }

  /** Repeat a move `n` times or throw if impossible */
  def repeat(n: Int, move: Move[A]): Zipper[A] = tryRepeat(n, move).get

  /**
   * Repeat a move while the condition is satisfied
   *
   * @return the first zipper that does not satisfy the condition, or failure
   */
  def tryRepeatWhile(condition: A ⇒ Boolean, move: Move[A]): Zipper.MoveResult[A] =
    if (!condition(focus)) stay else move(this) match {
      case MoveResult.Success(z) ⇒ z.tryRepeatWhile(condition, move)
      case failure ⇒ failure
    }

  /**
   * Repeat a move while the condition is satisfied or throw if impossible
   *
   * @return the first zipper that does not satisfy the condition
   */
  def repeatWhile(condition: A ⇒ Boolean, move: Move[A]): Zipper[A] =
    tryRepeatWhile(condition, move).get

  /**
   * Repeat a move while the condition is not satisfied
   *
   * @return the first zipper that satisfies the condition, or failure
   */
  def tryRepeatWhileNot(condition: A ⇒ Boolean, move: Move[A]): Zipper.MoveResult[A] =
    if (condition(focus)) stay else move(this) match {
      case MoveResult.Success(z) ⇒ z.tryRepeatWhileNot(condition, move)
      case failure ⇒ failure
    }

  /**
   * Repeat a move while the condition is not satisfied or throw if impossible
   *
   * @return the first zipper that satisfies the condition
   */
  def repeatWhileNot(condition: A ⇒ Boolean, move: Move[A]): Zipper[A] =
    tryRepeatWhileNot(condition, move).get

  /** Loop and accumulate state until a failure is produced */
  @tailrec final def loopAccum[B](acc: B)(f: (Zipper[A], B) ⇒ (Zipper.MoveResult[A], B)): (Zipper[A], B) = {
    f(this, acc) match {
      case (Zipper.MoveResult.Success(moved), accumulated) ⇒ moved.loopAccum(accumulated)(f)
      case (Zipper.MoveResult.Failure(prev), accumulated) ⇒ (prev, accumulated)
    }
  }
}

object Zipper {
  type Move[A] = Zipper[A] ⇒ MoveResult[A]

  /** A result of moving a zipper, which can be either successful or not */
  sealed trait MoveResult[A] {
    import MoveResult._

    /** Obtain the resulting zipper or throw an exception if the move failed */
    def get = this match {
      case Success(zipper) ⇒ zipper
      case Failure(_) ⇒ throw new UnsupportedOperationException("failed to move the zipper")
    }

    /** Obtain the resulting zipper or None if the move failed */
    def toOption = this match {
      case Success(zipper) ⇒ Some(zipper)
      case Failure(_) ⇒ None
    }

    /** Obtain the resulting zipper or the original zipper in case the move failed */
    def orStay = this match {
      case Success(zipper) ⇒ zipper
      case Failure(prev) ⇒ prev
    }

    /** Try another move if the current move failed */
    def orElse(other: Zipper[A] ⇒ MoveResult[A]): MoveResult[A] = this match {
      case Failure(prev) ⇒ other(prev)
      case success ⇒ success
    }

    /** Try a result of another move if the current move failed */
    def orElse(other: ⇒ MoveResult[A]): MoveResult[A] = this match {
      case Failure(_) ⇒ other
      case success ⇒ success
    }

    /** Try another safe move if the current move failed */
    def getOrElse(other: Zipper[A] ⇒ Zipper[A]): Zipper[A] = this match {
      case Success(zipper) ⇒ zipper
      case Failure(prev) ⇒ other(prev)
    }

    /** Try another zipper if the current move failed */
    def getOrElse(other: Zipper[A]): Zipper[A] = this match {
      case Success(zipper) ⇒ zipper
      case Failure(prev) ⇒ other
    }

    /** Safely move the resulting zipper, if the current move did not fail */
    def map(f: Zipper[A] ⇒ Zipper[A]) = this match {
      case Success(zipper) ⇒ Success(f(zipper))
      case failure ⇒ failure
    }

    /** Try another move on the resulting zipper, if the current move did not fail */
    def flatMap(f: Zipper[A] ⇒ MoveResult[A]) = this match {
      case Success(zipper) ⇒ f(zipper)
      case failure ⇒ failure
    }
  }

  object MoveResult {
    case class Success[A](zipper: Zipper[A]) extends MoveResult[A]
    case class Failure[A](prev: Zipper[A]) extends MoveResult[A]
  }

  /** Create a zipper from a tree-like data structure */
  def apply[A: Unzip](node: A): Zipper[A] = new Zipper(Nil, node, Nil, None)
}

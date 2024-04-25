package zipper

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

class ZipperSpec extends AnyFlatSpec with Matchers {
  case class Tree(x: Int, c: List[Tree] = List.empty)

  val tree = Tree(
    1, List(
      Tree(11, List(
        Tree(111),
        Tree(112)
      )),
      Tree(12, List(
        Tree(121),
        Tree(122, List(
          Tree(1221),
          Tree(1222)
        )),
        Tree(123)
      )),
      Tree(13)
    )
  )

  it should "perform basic operations correctly" in {
    val modified = Zipper(tree)
      .moveDownAt(1)                              .tapFocus(_.x shouldEqual 12)
      .moveDownRight                              .tapFocus(_.x shouldEqual 123)
      .deleteAndMoveLeft                          .tapFocus(_.x shouldEqual 122)
      .moveDownLeft                               .tapFocus(_.x shouldEqual 1221)
      .update(_.copy(x = -1))                     .tapFocus(_.x shouldEqual -1)
      .moveRight                                  .tapFocus(_.x shouldEqual 1222)
      .set(Tree(-2))                              .tapFocus(_.x shouldEqual -2)
      .moveUp                                     .tapFocus(_.x shouldEqual 122)
      .moveUp                                     .tapFocus(_.x shouldEqual 12)
      .rewindLeft                                 .tapFocus(_.x shouldEqual 11)
      .moveDownRight                              .tapFocus(_.x shouldEqual 112)
      .moveLeftBy(1)                              .tapFocus(_.x shouldEqual 111)
      .deleteAndMoveUp                            .tapFocus(_.x shouldEqual 11)
      .insertDownRight(List(Tree(113), Tree(114))).tapFocus(_.x shouldEqual 114)
      .moveUp                                     .tapFocus(_.x shouldEqual 11)
      .rewindRight                                .tapFocus(_.x shouldEqual 13)
      .insertDownLeft(List(Tree(131), Tree(132))) .tapFocus(_.x shouldEqual 131)
      .commit

    modified shouldEqual Tree(
      1, List(
        Tree(11, List(
          Tree(112),
          Tree(113),
          Tree(114)
        )),
        Tree(12, List(
          Tree(121),
          Tree(122, List(
            Tree(-1),
            Tree(-2)
          ))
        )),
        Tree(13, List(
          Tree(131),
          Tree(132)
        ))
      )
    )
  }

  it should "support depth-first traversal" in {
    Zipper(tree)
      .advanceRightDepthFirst.tapFocus(_.x shouldEqual 11)
      .advanceRightDepthFirst.tapFocus(_.x shouldEqual 111)
      .advanceRightDepthFirst.tapFocus(_.x shouldEqual 112)
      .advanceRightDepthFirst.tapFocus(_.x shouldEqual 12)

    Zipper(tree)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 13)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 12)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 123)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 122)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 1222)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 1221)
      .advanceLeftDepthFirst.tapFocus(_.x shouldEqual 121)

    val trimmed = Zipper(tree)
      .repeat(4, _.tryAdvanceLeftDepthFirst).tapFocus(_.x shouldEqual 122)
      .deleteAndAdvanceRightDepthFirst.tapFocus(_.x shouldEqual 123)
      .deleteAndAdvanceRightDepthFirst.tapFocus(_.x shouldEqual 13)
      .cycle(_.tryMoveUp)
      .repeat(3, _.tryAdvanceRightDepthFirst).tapFocus(_.x shouldEqual 112)
      .deleteAndAdvanceLeftDepthFirst.tapFocus(_.x shouldEqual 111)
      .tryDeleteAndAdvanceLeftDepthFirst.orStay.tapFocus(_.x shouldEqual 111)
      .commit

    trimmed shouldEqual Tree(
      1, List(
        Tree(11, List(
          Tree(111)
        )),
        Tree(12, List(
          Tree(121)
        )),
        Tree(13)
      )
    )
  }

  it should "allow to express loops in a simple way" in {
    Zipper(tree)
      .cycle(_.tryMoveDownRight, _.tryMoveLeft, _.tryMoveLeft)
      .tapFocus(_.x shouldEqual 111)
      .repeat(2, _.tryMoveUp)
      .tapFocus(_.x shouldEqual 1)

    Zipper(tree)
      .repeatWhile(_.x < 100, _.tryMoveDownLeft.flatMap(_.tryMoveRight))
      .tapFocus(_.x shouldEqual 122)
  }

  it should "allow to accumulate state while looping" in {
    def next: Zipper.Move[Tree] =
      _.tryMoveDownLeft
        .orElse(_.tryMoveRight)
        .orElse(_.tryMoveUp.flatMap(_.tryMoveRight))

    val (zipper, sum) = Zipper(tree)
      .repeatWhileNot(_.x > 10, next)
      .tapFocus(_.x shouldEqual 11)
      .loopAccum(0) { (z, a) =>
        if (a > 1000) (z.fail, a)
        else (next(z), a + z.focus.x)
      }

    zipper.focus.x shouldEqual 1222
    sum shouldEqual 1710
  }

  it should "throw when the move is impossible" in {
    intercept[UnsupportedOperationException] {
      Zipper(tree).moveUp
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).deleteAndMoveUp
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownRight.moveRight
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownRight.deleteAndMoveRight
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownLeft.moveLeft
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownLeft.deleteAndMoveLeft
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownRight.moveDownLeft
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownRight.insertDownLeft(List.empty)
    }

    intercept[UnsupportedOperationException] {
      Zipper(tree).moveDownRight.insertDownRight(List.empty)
    }
  }

  it should "allow to recover impossible moves" in {
    Zipper(tree).tryMoveUp.toOption shouldEqual None
    Zipper(tree).tryMoveDownLeft.toOption.isDefined shouldEqual true

    val modified1 = Zipper(tree)
      .moveDownLeft
      .tryMoveLeft.getOrElse(_.insertLeft(Tree(-1)).moveLeft)
      .commit

    modified1 shouldEqual Tree(
      1, List(
        Tree(-1),
        Tree(11, List(
          Tree(111),
          Tree(112)
        )),
        Tree(12, List(
          Tree(121),
          Tree(122, List(
            Tree(1221),
            Tree(1222)
          )),
          Tree(123)
        )),
        Tree(13)
      )
    )

    val modified2 = Zipper(tree)
      .moveDownLeft
      .tryMoveLeft.orStay
      .set(Tree(-1))
      .commit

    modified2 shouldEqual Tree(
      1, List(
        Tree(-1),
        Tree(12, List(
          Tree(121),
          Tree(122, List(
            Tree(1221),
            Tree(1222)
          )),
          Tree(123)
        )),
        Tree(13)
      )
    )

    val modified3 = Zipper(tree)
      .moveDownLeft
      .cycle(_.tryDeleteAndMoveRight)
      .tryDeleteAndMoveLeft.orElse(_.tryDeleteAndMoveRight).getOrElse(_.deleteAndMoveUp)
      .commit

    modified3 shouldEqual Tree(1)

    val modified4 = Zipper(tree).tryMoveDownLeft.flatMap(_.tryMoveUp).orStay.set(Tree(-1)).commit

    modified4 shouldEqual Tree(-1)
  }
}

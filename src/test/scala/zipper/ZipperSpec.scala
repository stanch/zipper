package zipper

import org.scalatest.{FlatSpec, Matchers}

class ZipperSpec extends FlatSpec with Matchers {
  case class Tree(x: Int, c: List[Tree] = List.empty)
  implicit val unzip = implicitly[Unzip[Tree]]

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
      .moveDownAt(1)          // 12
      .moveDownRight          // 123
      .deleteAndMoveLeft      // 122
      .moveDownLeft           // 1221
      .update(_.copy(x = -1))
      .moveRight              // 1222
      .set(Tree(-2))
      .moveUp                 // 122
      .moveUp                 // 12
      .rewindLeft             // 11
      .moveDownRight          // 112
      .moveLeftBy(1)          // 111
      .deleteAndMoveUp        // 11
      .commit

    modified shouldEqual Tree(
      1, List(
        Tree(11, List(
          Tree(112)
        )),
        Tree(12, List(
          Tree(121),
          Tree(122, List(
            Tree(-1),
            Tree(-2)
          ))
        )),
        Tree(13)
      )
    )
  }

  it should "allow to express loops in a simple way" in {
    val modified = Zipper(tree)
      .loopWhile(_.x < 1222, _.tryMoveDownLeft, _.tryMoveRight)
      .deleteAndMoveRight
      .commit

    modified shouldEqual Tree(
      1, List(
        Tree(11, List(
          Tree(111),
          Tree(112)
        )),
        Tree(12, List(
          Tree(121),
          Tree(122, List(
            Tree(1222)
          )),
          Tree(123)
        )),
        Tree(13)
      )
    )
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
  }

  it should "allow to recover impossible moves" in {
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
      .loop(_.tryDeleteAndMoveRight)
      .tryDeleteAndMoveLeft.orElse(_.tryDeleteAndMoveRight).getOrElse(_.deleteAndMoveUp)
      .commit

    modified3 shouldEqual Tree(1)

    val modified4 = Zipper(tree).tryMoveDownLeft.flatMap(_.tryMoveUp).orStay.set(Tree(-1)).commit

    modified4 shouldEqual Tree(-1)
  }
}

package zipper

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

class UnzipDerivationSpec extends AnyFlatSpec with Matchers {
  it should "derive Unzip for list-based trees" in {
    case class Tree(x: Int, c: List[Tree] = List.empty)

    val before = Tree(1, List(Tree(2)))
    val after = Tree(1, List(Tree(2), Tree(3)))

    Zipper(before).moveDownRight.insertRight(Tree(3, Nil)).commit shouldEqual after
  }

  it should "support other collections with a bit of boilerplate" in {
    case class Tree(x: Int, c: Vector[Tree] = Vector.empty)

    val before = Tree(1, Vector(Tree(2)))
    val after = Tree(1, Vector(Tree(2), Tree(3)))

    implicit val unzip = Unzip.For[Tree, Vector].derive

    Zipper(before).moveDownRight.insertRight(Tree(3)).commit shouldEqual after
  }
}

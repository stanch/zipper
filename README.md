## Zipper — an implementation of Huet’s Zipper

A Zipper is a tool that allows to navigate and modify immutable recursive data structures.
This implementation is inspired by
[the original paper by Huet](https://www.st.cs.uni-saarland.de/edu/seminare/2005/advanced-fp/docs/huet-zipper.pdf),
as well as the [Argonaut’s JSON Zipper](http://argonaut.io/doc/zipper/).

Consider the following example:

```scala
// Define a tree data structure
case class Tree(x: Int, c: List[Tree] = List.empty)

// Create a tree
val tree = Tree(
  1, List(
    Tree(
      11, List(
        Tree(111),
        Tree(112)
      )
    ),
    Tree(
      12, List(
        Tree(121),
        Tree(
          122, List(
            Tree(1221),
            Tree(1222)
          )
        ),
        Tree(123)
      )
    ),
    Tree(13)
  )
)
```

Let’s use [reftree](https://github.com/stanch/reftree) to have a better look:

```scala
import java.nio.file.Paths
import reftree._

// Simplify list visualization to reduce visual noise
import ToRefTree.Simple.list

DotPlotter(Paths.get("images", "tree.png"), verticalSpacing = 1).plot(tree)
```

<img src="images/tree.png" height="500px" alt="a tree" />

Since the tree is immutable, modifying it can be a pain,
but it’s easily solved with a Zipper:

```scala
import zipper._

// Use a Zipper to move around and change data
val modified = {
  Zipper(tree)
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
    .commit                 // commit the changes and return the result
}
```

Here’s what the modified tree looks like:

```scala
DotPlotter(Paths.get("images", "modified.png"), verticalSpacing = 1).plot(modified)
```

<img src="images/modified.png" height="500px" alt="a modified tree" />

If we draw both trees side by side, we’ll see that
the unchanged parts are shared:

```scala
DotPlotter(Paths.get("images", "both.png"), verticalSpacing = 1).plot(tree, modified)
```

<img src="images/both.png" height="500px" alt="both trees" />

_Since all the example code is actually run by [tut](https://github.com/tpolecat/tut),
you can find the resulting images in the `images` directory._


### Usage

#### Unzip

In order for the Zipper to work on your data structure `Tree`, you need an implicit instance of `Unzip[Tree]`.
`Unzip` is defined as follows:

```scala
trait Unzip[A] {
  def unzip(node: A): List[A]
  def zip(node: A, children: List[A]): A
}
```

As we saw before, the library can automatically derive `Unzip[Tree]`
if the `Tree` is a case class that has a single field of type `List[Tree]`.
It is also possible to derive an `Unzip[Tree]` for similar cases, but with other collections:

```scala
scala> case class Tree(x: Int, c: Vector[Tree] = Vector.empty)
defined class Tree

scala> implicit val unzip = Unzip.For[Tree, Vector].derive
unzip: zipper.Unzip[Tree] = zipper.GenericUnzipInstances$For$$anon$2@2e7cb857
```

The automatic derivation is powered by [shapeless](https://github.com/milessabin/shapeless).

#### Moves, failures and recovery

There are many operations defined on a `Zipper`.
Some of them are not safe, e.g. `moveLeft` will fail with an exception
if there are no elements on the left.
For all unsafe operations a safe version is provided, which is prefixed with `try`.
These operations return a `Zipper.MoveResult`, which allows to recover from the failure or return to the previous state:

```scala
scala> val tree = Tree(1, Vector(Tree(3), Tree(4)))
tree: Tree = Tree(1,Vector(Tree(3,Vector()), Tree(4,Vector())))

scala> val modified = {
     |   Zipper(tree)
     |     .moveDownLeft
     |     .tryMoveLeft.getOrElse(_.insertLeft(Tree(2)).moveLeft)
     |     .commit
     | }
modified: Tree = Tree(1,Vector(Tree(2,Vector()), Tree(3,Vector()), Tree(4,Vector())))
```

#### Loops

`Zipper` provides a looping functionality, which can be useful with recursive data:

```scala
scala> val tree = Tree(1, Vector(Tree(2), Tree(3), Tree(5)))
tree: Tree = Tree(1,Vector(Tree(2,Vector()), Tree(3,Vector()), Tree(5,Vector())))

scala> val modified = {
     |   Zipper(tree)
     |     .moveDownLeft
     |     .loopWhile(_.x < 5, _.tryMoveRight)
     |     .insertRight(Tree(4))
     |     .commit
     | }
modified: Tree = Tree(1,Vector(Tree(2,Vector()), Tree(3,Vector()), Tree(4,Vector()), Tree(5,Vector())))
```

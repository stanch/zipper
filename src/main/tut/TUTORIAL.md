## Zipper tutorial

Suppose we have a tree structure:

```tut:invisible
import java.nio.file.Paths
import reftree._
import ToRefTree.Simple.list
val path = Paths.get("images/tutorial")
```

```tut:silent
case class Tree(x: Int, c: List[Tree] = List.empty)

val tree1 = Tree(
  1, List(
    Tree(2),
    Tree(3),
    Tree(4),
    Tree(5)
  )
)
```

```tut:invisible
DotPlotter(path.resolve("tree1.png")).plot(tree1)
```

<p align="center"><img src="images/tutorial/tree1.png" width="50%" /></p>

When we wrap a Zipper around this tree, it does not look very interesting yet:

```tut:silent
import zipper._

val zipper1 = Zipper(tree1)
```

```tut:invisible
DotPlotter(path.resolve("zipper1.png")).plot(tree1, zipper1)
```

<p align="center"><img src="images/tutorial/zipper1.png" width="50%" /></p>

We can see that it just points to the original tree and has some other empty fields.
More specifically, a Zipper consists of four pointers:

* The current focus
* Left siblings of the focus
* Right siblings of the focus
* The parent zipper

In this case the focus is the root of the tree, which has no siblings,
and the parent zipper does not exist, since we are at the top level.

One thing we can do right away is modify the focus:

```tut:silent
val zipper2 = zipper1.update(focus ⇒ focus.copy(x = focus.x + 99))
```

```tut:invisible
DotPlotter(path.resolve("zipper2.png")).plot(tree1, zipper1, zipper2)
```

<p align="center"><img src="images/tutorial/zipper2.png" width="50%" /></p>

We just created a new tree! To obtain it, we have to commit the changes:

```tut:silent
val tree2 = zipper2.commit
```

```tut:invisible
DotPlotter(path.resolve("tree2.png")).plot(tree1, tree2)
```

<p align="center"><img src="images/tutorial/tree2.png" width="50%" /></p>

If you were following closely,
you would notice that nothing spectacular happened yet:
we could’ve easily obtained the same result by modifying the tree directly:

```tut:silent
val tree2b = tree1.copy(x = tree1.x + 99)

assert(tree2b == tree2)
```

The power of Zipper becomes apparent when we go one or more levels deep.
To move down the tree, we “unzip” it, separating the child nodes into
the focused node and its left and right siblings:

```tut:silent
val zipper2 = zipper1.moveDownLeft
```

```tut:invisible
DotPlotter(path.resolve("zipper1+2.png")).plot(zipper1, zipper2)
```

<p align="center"><img src="images/tutorial/zipper1+2.png" width="50%" /></p>

The new Zipper links to the old one,
which will allow us to return to the root of the tree when we are done applying changes.
This link however prevents us from seeing the picture clearly.
Let’s suppress the parent field:

```tut:invisible
val oldVisualization = implicitly[ToRefTree[Zipper[Tree]]]
implicit val newVisualization = oldVisualization.suppressField(2)
```

```tut:invisible
DotPlotter(path.resolve("zipper2b.png")).plot(zipper2)
```

<p align="center"><img src="images/tutorial/zipper2b.png" width="50%" /></p>

Great! We have `2` in focus and `3, 4, 5` as right siblings. What happens if we move right a bit?

```tut:silent
val zipper3 = zipper2.moveRightBy(2)
```

```tut:invisible
DotPlotter(path.resolve("zipper3.png")).plot(zipper3)
```

<p align="center"><img src="images/tutorial/zipper3.png" width="50%" /></p>

This is interesting! Notice that the left siblings are “inverted”.
This allows to move left and right in constant time, because the sibling
adjacent to the focus is always at the head of the list.

This also allows us to insert new siblings easily:

```tut:silent
val zipper4 = zipper3.insertLeft(Tree(34))
```

```tut:invisible
DotPlotter(path.resolve("zipper4.png")).plot(zipper4)
```

<p align="center"><img src="images/tutorial/zipper4.png" width="50%" /></p>

And, as you might know, we can delete nodes and update the focus:

```tut:silent
val zipper5 = zipper4.deleteAndMoveRight.set(Tree(45))
```

```tut:invisible
DotPlotter(path.resolve("zipper5.png")).plot(zipper5)
```

<p align="center"><img src="images/tutorial/zipper5.png" width="50%" /></p>

Finally, when we move up, the siblings at the current level are “zipped”
together and their parent node is updated:

```tut:silent
val zipper6 = zipper5.moveUp
```

```tut:invisible
DotPlotter(path.resolve("zipper6.png")).plot(zipper6)
```

<p align="center"><img src="images/tutorial/zipper6.png" width="50%" /></p>

You can probably guess by now that `.commit` is a shorthand for going
all the way up (applying all the changes) and returning the focus:

```tut:silent
val tree3a = zipper5.moveUp.focus
val tree3b = zipper5.commit

assert(tree3a == tree3b)
```

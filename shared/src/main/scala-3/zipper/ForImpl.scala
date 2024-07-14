package zipper

import contrib.shapeless3.{Replacer, Selector}
import shapeless3.deriving.K0
import shapeless3.deriving.K0.*

import scala.collection.Factory

private[zipper] trait ForImpl {
  given unzipListBased[A, L <: Tuple](using
    generic: K0.ProductGeneric[A] { type MirroredElemTypes = L },
    selector: Selector[L, List[A]],
    replacer: Replacer.Aux[L, List[A], List[A], (List[A], L)]
  ): Unzip[A] with {
    def unzip(node: A): List[A] = selector(generic.toRepr(node))
    def zip(node: A, children: List[A]): A = {
      val repr = replacer(generic.toRepr(node), children)
      generic.fromRepr(repr._2)
    }
  }

  class For[A, Coll[X] <: Seq[X]]:
    /** Derive an instance of `Unzip[A]` */
    inline given derive[L <: Tuple](using
      generic: K0.ProductGeneric[A] { type MirroredElemTypes = L },
      selector: Selector[L, Coll[A]],
      replacer: Replacer.Aux[L, Coll[A], Coll[A], (Coll[A], L)],
      factory: Factory[A, Coll[A]]
    ): Unzip[A] with {
      def unzip(node: A): List[A] = selector(generic.toRepr(node)).toList
      def zip(node: A, children: List[A]): A = {
        val repr = replacer(generic.toRepr(node), children.to(factory))
        generic.fromRepr(repr._2)
      }
  }

  object For:
    /**
     * @tparam A    The type of the tree-like data structure
     * @tparam Coll The type of the collection used for recursion (e.g. Vector)
     */
    def apply[A, Coll[X] <: Seq[X]]: For[A, Coll] = new For[A, Coll]
}

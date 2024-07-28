package contrib.shapeless3

/**
 * This is ported from [[shapeless.ops.hlist.Replacer Replacer]] from shapeless-2.
 * At the moment of implementation, there is no direct support in shapeless-3.
 * We should give up on it once it arrives in the library.
 */
trait Replacer[L <: Tuple, U, V]:
  type Out <: Tuple
  def apply(t: L, v: V): Out

object Replacer:
  def apply[L <: Tuple, U, V](using r: Replacer[L, U, V]): Aux[L, U, V, r.Out] = r

  type Aux[L <: Tuple, U, V, Out0] = Replacer[L, U, V] { type Out = Out0 }

  given tupleReplacer1[T <: Tuple, U, V]: Aux[U *: T, U, V, (U, V *: T)] =
    new Replacer[U *: T, U, V] {
      type Out = (U, V *: T)

      def apply(l: U *: T, v: V): Out = (l.head, v *: l.tail)
    }

  given tupleReplacer2[H, T <: Tuple, U, V, OutT <: Tuple](using
    ut: Aux[T, U, V, (U, OutT)]): Aux[H *: T, U, V, (U, H *: OutT)] =
      new Replacer[H *: T, U, V] {
        type Out = (U, H *: OutT)

        def apply(l: H *: T, v: V): Out = {
          val (u, outT) = ut(l.tail, v)
          (u, l.head *: outT)
        }
      }

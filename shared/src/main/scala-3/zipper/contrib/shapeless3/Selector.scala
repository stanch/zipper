package contrib.shapeless3

/**
 * This is ported from [[shapeless.ops.hlist.Selector Selector]] from shapeless-2.
 * At the moment of implementation, there is no direct support in shapeless-3.
 * We should give up on it once it arrives in the library.
 */
trait Selector[L <: Tuple, U]:
  def apply(t: L): U

object Selector:
  given[H, T <: Tuple]: Selector[H *: T, H] with {
    def apply(t: H *: T): H = t.head
  }

  given[H, T <: Tuple, U] (using s: Selector[T, U]): Selector[H *: T, U] with {
    def apply(t: H *: T): U = s (t.tail)
}

package contrib.shapeless3

trait Selector[L <: Tuple, U]:
  def apply(t: L): U

object Selector:
  given[H, T <: Tuple]: Selector[H *: T, H] with {
    def apply(t: H *: T): H = t.head
  }

  given[H, T <: Tuple, U] (using s: Selector[T, U]): Selector[H *: T, U] with {
    def apply(t: H *: T): U = s (t.tail)
}

package uuverifiers.common

import uuverifiers.catra.Counter

sealed trait Transition {
  def isSelfLoop(): Boolean = from == to
  def intersect[T <: Transition](that: T): Option[ProductTransition]
  def from(): State
  def to(): State
  def label(): SymbolicLabel
  def affectsCounters(): Set[Counter] = ???
  def increments(_c: Counter): Option[Int] = ???
  def isProductOf(_that: Transition): Boolean = ???
  def originTransitions(): Option[(Transition, Transition)] = ???

  /**
   * Add this set of counter increments to this transition, making it a
   *  Counting transition.
   * @param increments
   * @return a new Counting transition with these increments
   */
  def withIncrements(increments: Map[Counter, Int]): Counting =
    new Counting(this, increments)
}

sealed class SymbolicTransition(
    _from: State,
    _label: SymbolicLabel,
    _to: State
) extends Transition {
  override def intersect[T <: Transition](
      that: T
  ): Option[ProductTransition] = {
    val newTransition = new ProductTransition(this, that)
    if (!newTransition.label.isEmpty)
      Some(newTransition)
    else None
  }
  override def from(): State = _from
  override def to(): State = _to
  override def label(): SymbolicLabel = _label
}
object Transition {
  def unapply(t: Transition): Some[(State, SymbolicLabel, State)] =
    Some((t.from(), t.label(), t.to()))
}

sealed case class Counting(
    baseTransition: Transition,
    counterIncrements: Map[Counter, Int]
) extends SymbolicTransition(
      baseTransition.from(),
      baseTransition.label(),
      baseTransition.to()
    ) {
  override def affectsCounters(): Set[Counter] = counterIncrements.keySet
  override def increments(c: Counter): Option[Int] = counterIncrements.get(c)
}

object Counting {
  def apply(
      from: State,
      label: SymbolicLabel,
      increments: Map[Counter, Int],
      to: State
  ): Counting =
    new Counting(new SymbolicTransition(from, label, to), increments)

  def apply(base: SymbolicTransition, increments: Map[Counter, Int]): Counting =
    new Counting(base, increments)
}

sealed class ProductTransition(
    left: Transition,
    right: Transition
) extends SymbolicTransition(
      _from = left.from().intersect(right.from()),
      _label = left.label().intersect(right.label()),
      _to = left.to().intersect(right.to())
    ) {

  override def isProductOf(transition: Transition): Boolean =
    left == transition || right == transition
  override def originTransitions(): Option[(Transition, Transition)] =
    Some((left, right))
  override def from(): State = left.from() intersect right.from()
  override def to(): State = left.to() intersect right.to()
  override def affectsCounters(): Set[Counter] =
    left.affectsCounters() union right.affectsCounters()
  override def increments(c: Counter): Option[Int] = left increments c match {
    case i @ Some(_) => i
    case None        => right increments c
  }
}

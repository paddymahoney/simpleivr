package simpleivr

import java.io.File
import java.time.LocalTime


abstract class Sayables(val ttsDir: File) extends Speaks {
  lazy val
    `An error occurred.`,
    `Please say`,
    `after the tone, and press pound when finished.`,
    `is`,
    `Is that correct?`,
    `You must enter `,
    `You must enter at least`,
    `Please make a selection`,
    `That is not one of the choices.`,
    `Press 1 for yes, or 2 for no.`,
    `Press`,
    `star`,
    `pound`,
    `For more choices`,
    `For the previous choices`,
    `for any of the following:`,
    `To return to the previous menu`,
    `digit`,
    `digits`,
    `You cannot enter more than`,
    `That entry is not valid`,
    `and`,
    `or`,
    `now`,
    `Please enter`,
    `A`,
    `M`,
    `O`,
    `P`,
    `clock`
      = speak

  lazy val
    `zero`, `one`, `two`, `three`, `four`, `five`,
    `six`, `seven`, `eight`, `nine`, `ten`,
    `eleven`, `twelve`, `thirteen`, `fourteen`, `fifteen`,
    `sixteen`, `seventeen`, `eighteen`, `nineteen`, `twenty`,
    `thirty`, `forty`, `fifty`, `sixty`, `seventy`, `eighty`, `ninety`,
    `hundred`, `thousand`, `million`, `negative`
      = speak


  def numberWords(number: Int): Sayable = {
    /*
     * Based on an article by Richard Carr, at http://www.blackwasp.co.uk/NumberToWords.aspx
     */
    val smallNumbers = collection.IndexedSeq(
      `one`, `two`, `three`, `four`, `five`, `six`, `seven`, `eight`, `nine`, `ten`,
      `eleven`, `twelve`, `thirteen`, `fourteen`, `fifteen`, `sixteen`, `seventeen`, `eighteen`, `nineteen`
    )

    // Tens number names from twenty upwards
    val _tens = collection.IndexedSeq(
      `twenty`, `thirty`, `forty`, `fifty`, `sixty`, `seventy`, `eighty`, `ninety`
    )

    // Scale number names for use during recombination
    val scaleNumbers = List(SayNothing, `thousand`, `million`)

    def ifs(cond: Boolean)(s: => Sayable) = if (cond) s else SayNothing

    if (number == 0) `zero` else {
      @annotation.tailrec def split(num: Int)(agg: List[Int]): List[Int] =
        if (num == 0) agg
        else split(num / 1000)((num % 1000) :: agg)

      val digitGroups = split(math.abs(number))(Nil).reverse

      val groupText = digitGroups map { num =>
        val hundreds = num / 100
        val tensUnits = num % 100
        val tens = tensUnits / 10
        val units = tensUnits % 10

        val text = ifs(hundreds != 0)(smallNumbers(hundreds - 1) & `hundred` & ifs(tensUnits != 0)(`and`)) &
          ifs(tens >= 2)(_tens(tens - 2) & ifs(units != 0)(smallNumbers(units - 1))) &
          ifs(tens < 2 && tensUnits != 0)(smallNumbers(tensUnits - 1))

        (num, text)
      }

      def render(groups: List[(Int, Sayable)], scales: List[Sayable], first: Boolean): Sayable = groups match {
        case Nil                 =>
          SayNothing
        case (num, text) :: rest =>
          val next = render(rest, scales.tail, first = false)
          val cur = ifs(num > 0)(text & scales.head)
          val sep = ifs(SayNothing != next && SayNothing != cur)(Pause(250) & ifs(first && num > 0 && num < 100)(`and`))
          next & sep & cur
      }

      ifs(number < 0)(`negative`) & render(groupText, scaleNumbers, first = true)
    }
  }

  def digitWords(s: String) = s.toSeq.map(c => numberWords(c - '0'))

  def charWord(c: Char): Sayable = c match {
    case '#'                             => `pound`
    case '*'                             => `star`
    case chr if chr >= '0' && chr <= '9' => digitWords(chr.toString)
    // TODO other case? log error? use better type than Char?
  }

  def timeWords(time: LocalTime) =
    numberWords((time.getHour + 11) % 12 + 1) &
      (if (time.getMinute < 10) `O` else SayNothing) &
      (if (time.getMinute == 0) `clock` else numberWords(time.getMinute)) &
      (if (time.getHour < 12) `A` else `P`) & `M`
}

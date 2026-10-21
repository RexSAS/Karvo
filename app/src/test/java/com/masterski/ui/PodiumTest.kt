package com.masterski.ui

import com.masterski.domain.StudentStat
import com.masterski.ui.overview.Ranked
import com.masterski.ui.overview.podiumOrder
import com.masterski.ui.overview.runnersUp
import org.junit.Assert.assertEquals
import org.junit.Test

class PodiumTest {
    private fun s(name: String, minutes: Long) = StudentStat(name, minutes, null)
    private val five = listOf(s("A", 500), s("B", 400), s("C", 300), s("D", 200), s("E", 100))

    @Test fun podiumShowsSecondFirstThird() =
        assertEquals(listOf(Ranked(2, five[1]), Ranked(1, five[0]), Ranked(3, five[2])), podiumOrder(five))

    @Test fun podiumWithTwoStudentsShowsSecondThenFirst() =
        assertEquals(listOf(Ranked(2, five[1]), Ranked(1, five[0])), podiumOrder(five.take(2)))

    @Test fun podiumWithOneStudentShowsOnlyFirst() =
        assertEquals(listOf(Ranked(1, five[0])), podiumOrder(five.take(1)))

    @Test fun podiumEmptyWithoutStudents() = assertEquals(emptyList<Ranked>(), podiumOrder(emptyList()))

    @Test fun runnersUpAreFourthAndFifth() =
        assertEquals(listOf(Ranked(4, five[3]), Ranked(5, five[4])), runnersUp(five))

    @Test fun noRunnersUpWithThreeOrFewer() = assertEquals(emptyList<Ranked>(), runnersUp(five.take(3)))
}

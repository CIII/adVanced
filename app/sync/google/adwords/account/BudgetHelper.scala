package sync.google.adwords.account

import Shared.Shared._
import akka.event.LoggingAdapter
import sync.google.adwords.AdWordsHelper
import com.google.api.ads.adwords.axis.v201609.cm.BudgetServiceInterface
import com.google.api.ads.adwords.axis.v201609.cm.Budget
import com.google.api.ads.adwords.axis.v201609.cm.BudgetReturnValue
import com.google.api.ads.adwords.axis.v201609.cm.BudgetOperation
import com.google.api.ads.adwords.axis.v201609.cm.Operator
import com.google.api.ads.adwords.axis.v201609.cm.Money

class BudgetHelper(AdWords: AdWordsHelper, log: LoggingAdapter) {
  private val budgetService = AdWords.adWordsServices.get(AdWords.adWordsSession, classOf[BudgetServiceInterface])
  
  def updateBudget(budget: Budget): BudgetReturnValue = {
    val op = new BudgetOperation
    op.setOperand(budget)
    op.setOperator(Operator.SET)
    budgetService.mutate(Array(op))
  }
  
  def createBudget(budget: Budget): BudgetReturnValue = {
    val op = new BudgetOperation
    op.setOperand(budget)
    op.setOperator(Operator.ADD)
    budgetService.mutate(Array(op))
  }
  
  def newBudget(dollarAmount: Double, name: String): Budget = {
    val budget = new Budget
    val money = new Money
    money.setMicroAmount(dollarsToMicro(dollarAmount))
    budget.setAmount(money)
    budget.setName(name)
    budget
  }
}
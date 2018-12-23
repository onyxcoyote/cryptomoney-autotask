/*
    This file is part of cryptomoney-autotask.

    cryptomoney-autotask is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    cryptomoney-autotask is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with cryptomoney-autotask.  If not, see <https://www.gnu.org/licenses/>.
 */
package cryptomoney.autotask.rule;

import com.coinbase.exchange.api.accounts.Account;
import cryptomoney.autotask.CryptomoneyAutotask;
import com.coinbase.exchange.api.payments.PaymentType;
import com.coinbase.exchange.api.entity.PaymentResponse;
import cryptomoney.autotask.functions.SharedFunctions;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_DepositUSD extends Rule
{
    private double maximumAvgOccurrencesPerDay;
    private double minimumUSDQuantityThreshold;
    private double maximumUSDQuantity;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)
    
    public RuleAction_DepositUSD()
    {
        super(RuleType.ACTION, ActionType.ACTION_DEPOSIT_USD);
    }
    
    public RuleAction_DepositUSD(boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumUSDQuantityThreshold, double _maximumUSDQuantity)
    {
        super(RuleType.ACTION, ActionType.ACTION_DEPOSIT_USD);
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumUSDQuantityThreshold = _minimumUSDQuantityThreshold;
        maximumUSDQuantity = _maximumUSDQuantity;
        
        if(_executeImmediately)
        {
            executionCount = (int)Math.ceil(getNumberOfExecutionsBeforeExecutingOnce());
        }
    }
    
    private double getNumberOfExecutionsBeforeExecutingOnce()
    {
        double systemExecutionsPerDay = SharedFunctions.GetNumberOfSystemIntervalsPerDay();
        double numberOfExecutionsBeforeExecutingOnce = systemExecutionsPerDay / maximumAvgOccurrencesPerDay;
        return numberOfExecutionsBeforeExecutingOnce;
    }
        
    @Override
    public void doAction()
    {
        //CryptomoneyAutotask.logProv.LogMessage(getHelpString());
        
        executionCount++;
        

        
        if(this.account.allowanceDepositUSD.getAllowance().doubleValue() < minimumUSDQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " account.getAllowanceDepositUSD() does not exceed minimumUSDQuantityThreshold " + this.account.allowanceDepositUSD.getAllowance() + "/" + minimumUSDQuantityThreshold);
            return;
        }


        
        double numberOfExecutionsBeforeExecutingOnce = getNumberOfExecutionsBeforeExecutingOnce();
        
        CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " execution count: " + executionCount + "/" + numberOfExecutionsBeforeExecutingOnce);
        if(executionCount < numberOfExecutionsBeforeExecutingOnce)
        {
            return; //keep waiting...
        }
        else
        {
            executionCount-=numberOfExecutionsBeforeExecutingOnce;
        }
        
        
        
        BigDecimal amountToDeposit = this.account.allowanceDepositUSD.getAllowance();
        if(amountToDeposit.doubleValue() > maximumUSDQuantity)
        {
           BigDecimal amountAboveMax = amountToDeposit.subtract(BigDecimal.valueOf(maximumUSDQuantity));
           amountToDeposit = amountToDeposit.subtract(amountAboveMax);
        }

        List<PaymentType> paymentTypes = CryptomoneyAutotask.paymentService.getPaymentTypes();
        
        PaymentType paymentTypeBank = null;
        for(PaymentType paymentType : paymentTypes)
        {
            CryptomoneyAutotask.logProv.LogMessage("paymentType account retrieved: " + paymentType.getAllow_buy() + " " + paymentType.getId() + " " + paymentType.getName() + " " + paymentType.getType()); //todo: keep this, shows last 4 digit bank#?
            if(paymentType.getCurrency().equals("USD") && paymentType.getPrimary_buy()) //todo: abstract away USD and BTC
            {
                if(paymentTypeBank != null)
                {
                    CryptomoneyAutotask.logProv.LogMessage("ERROR, TWO -PRIMARY BUY- BANK ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                    //TODO: in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                    System.exit(1);
                }
                paymentTypeBank = paymentType;
            }
        }
        
        if(paymentTypeBank == null)
        {
            CryptomoneyAutotask.logProv.LogMessage("ERROR, NO BANK PAYMENT TYPE FOUND");
            System.exit(1);
        }
        
        
        
        BigDecimal depositAboutBD = amountToDeposit.setScale(2, RoundingMode.HALF_EVEN);
        PaymentResponse response = CryptomoneyAutotask.depositService.depositViaPaymentMethod(depositAboutBD, "USD", paymentTypeBank.getId()); //API CALL
        CryptomoneyAutotask.logProv.LogMessage("Requested USD deposit, response: " + response.getCurrency() + " " + response.getAmount() + " " + response.getPayout_at());
        CryptomoneyAutotask.logProvFile.LogMessage("Requested USD deposit, response: " + response.getCurrency() + " " + response.getAmount() + " " + response.getPayout_at());
        this.account.allowanceDepositUSD.addToAllowance(amountToDeposit.negate());
        
        //purge any extra allowance
        if(this.account.allowanceDepositUSD.getAllowance().doubleValue() > 0)
        {
            CryptomoneyAutotask.logProv.LogMessage("purging USD deposit allowance " + this.account.allowanceDepositUSD.getAllowance());
            CryptomoneyAutotask.logProvFile.LogMessage("purging USD deposit allowance " + this.account.allowanceDepositUSD.getAllowance());
            this.account.allowanceDepositUSD.resetAllowance();
        }
        
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() + "";
    }
}


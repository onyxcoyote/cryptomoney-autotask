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
import cryptomoney.autotask.allowance.*;
import cryptomoney.autotask.currency.FiatCurrencyType;
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
    private FiatCurrencyType fiatCurrencyType;
    private double maximumAvgOccurrencesPerDay;
    private double minimumUSDQuantityThreshold;
    private double maximumUSDQuantity;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)
    
    public RuleAction_DepositUSD()
    {
        super(RuleType.ACTION, ActionType.ACTION_DEPOSIT_USD);
    }
    
    public RuleAction_DepositUSD(FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumUSDQuantityThreshold, double _maximumUSDQuantity)
    {
        super(RuleType.ACTION, ActionType.ACTION_DEPOSIT_USD);
        fiatCurrencyType = _fiatCurrencyType;
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumUSDQuantityThreshold = _minimumUSDQuantityThreshold;
        maximumUSDQuantity = _maximumUSDQuantity;
        
        if(_executeImmediately)
        {
            executionCount = (int)Math.ceil(getNumberOfExecutionsBeforeExecutingOnce());
        }
    }
    
    private AllowanceFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceFiat(AllowanceType.Deposit, fiatCurrencyType);
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
        

        
        if(getAssociatedAllowance().getAllowance().doubleValue() < minimumUSDQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " account.getAllowanceDepositUSD() does not exceed minimumUSDQuantityThreshold " + getAssociatedAllowance().getAllowance() + "/" + minimumUSDQuantityThreshold);
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
            
            for(int i=0;i<100 && executionCount > numberOfExecutionsBeforeExecutingOnce;i++)
            {
                if(executionCount > numberOfExecutionsBeforeExecutingOnce)
                {
                    executionCount-=numberOfExecutionsBeforeExecutingOnce; //don't let it run a bunch of times in a row
                }
            }
        }
        
        
        
        BigDecimal amountToDeposit = getAssociatedAllowance().getAllowance();
        if(amountToDeposit.doubleValue() > maximumUSDQuantity)
        {
           BigDecimal amountAboveMax = amountToDeposit.subtract(BigDecimal.valueOf(maximumUSDQuantity));
           amountToDeposit = amountToDeposit.subtract(amountAboveMax);
        }

        String paymentTypeBank_Id = this.account.getCoinbaseProUSDBankPaymentType_Id();
        
        if(paymentTypeBank_Id == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, NO BANK PAYMENT TYPE FOUND");
            System.exit(1);
        }
        
        
        
        
        BigDecimal depositAboutBD = amountToDeposit.setScale(2, RoundingMode.HALF_EVEN);
        PaymentResponse response = CryptomoneyAutotask.depositService.depositViaPaymentMethod(depositAboutBD, "USD", paymentTypeBank_Id); //API CALL
        CryptomoneyAutotask.logMultiplexer.LogMessage("Requested USD deposit, response: " + response.getCurrency() + " " + response.getAmount() + " " + response.getPayout_at());
        getAssociatedAllowance().addToAllowance(amountToDeposit.negate());
        
        //purge any extra allowance
        if(getAssociatedAllowance().getAllowance().doubleValue() > 0)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("purging USD deposit allowance " + getAssociatedAllowance().getAllowance());
            getAssociatedAllowance().resetAllowance();
        }
        
        
        CryptomoneyAutotask.logProv.LogMessage("");
    }
    
    @Override
    public String getHelpString()
    {
        return this.getRuleType() + " " + this.getActionType() 
            + " maximumAvgOccurrencesPerDay:" + maximumAvgOccurrencesPerDay
            + " minimumUSDQuantityThreshold:" + minimumUSDQuantityThreshold
            + " maximumUSDQuantity:" + maximumUSDQuantity
                ;
    }
}


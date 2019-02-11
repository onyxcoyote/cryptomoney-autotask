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
import cryptomoney.autotask.exchangeaccount.WalletAccountCurrency;
import cryptomoney.autotask.functions.SharedFunctions;

import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_DepositFiat extends Rule
{
    private FiatCurrencyType fiatCurrencyType;
    private double maximumAvgOccurrencesPerDay;
    private double minimumFiatQuantityThreshold;
    private double maximumFiatQuantity;
    
    private int executionCount = 0; //if we set this to 999999 then it would execute right away upon running program (maybe)
    
    public RuleAction_DepositFiat()
    {
        super(null, RuleType.ACTION, ActionType.ACTION_DEPOSIT_FIAT);
    }
    
    public RuleAction_DepositFiat(UUID _uuid, FiatCurrencyType _fiatCurrencyType, boolean _executeImmediately, double _maximumAvgOccurrencesPerDay, double _minimumFiatQuantityThreshold, double _maximumFiatQuantity)
    {
        super(_uuid, RuleType.ACTION, ActionType.ACTION_DEPOSIT_FIAT);
        fiatCurrencyType = _fiatCurrencyType;
        maximumAvgOccurrencesPerDay = _maximumAvgOccurrencesPerDay;
        minimumFiatQuantityThreshold = _minimumFiatQuantityThreshold;
        maximumFiatQuantity = _maximumFiatQuantity;
        
        if(_executeImmediately)
        {
            executionCount = (int)Math.ceil(getNumberOfExecutionsBeforeExecutingOnce());
        }
    }
    
    private AllowanceFiat getAssociatedAllowance()
    {
        return this.account.getAllowanceFiat(AllowanceType.Deposit, fiatCurrencyType, this.uuid);
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
        

        
        if(getAssociatedAllowance().getAllowance().doubleValue() < minimumFiatQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage(getHelpString() + " account.getAllowanceDepositUSD() does not exceed minimumFiatQuantityThreshold " + getAssociatedAllowance().getAllowance().setScale(3, RoundingMode.HALF_EVEN) + "/" + minimumFiatQuantityThreshold);
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
            
            if(executionCount > numberOfExecutionsBeforeExecutingOnce)
            {
                executionCount = (int)Math.round(executionCount % numberOfExecutionsBeforeExecutingOnce); //modulo - reduces it to a number less than numberOfExecutionsBeforeExecutingOnce to prevent it from running thrice (or more) in sequence in case the executionCount built up a lot.
            }
        }
        
        
        
        BigDecimal amountToDeposit = getAssociatedAllowance().getAllowance();
        if(amountToDeposit.doubleValue() > maximumFiatQuantity)
        {
           BigDecimal amountAboveMax = amountToDeposit.subtract(BigDecimal.valueOf(maximumFiatQuantity));
           amountToDeposit = amountToDeposit.subtract(amountAboveMax);
        }

        String paymentTypeBank_Id = this.account.getCoinbaseProPaymentType_AccountId(WalletAccountCurrency.valueOf(this.fiatCurrencyType.toString()));
        
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
            + " fiatCurrencyType:"+ this.fiatCurrencyType                
            + " execsPerDay:" + maximumAvgOccurrencesPerDay
            + " minFiatQty:" + minimumFiatQuantityThreshold
            + " maxFiatQty:" + maximumFiatQuantity
                ;
    }
}


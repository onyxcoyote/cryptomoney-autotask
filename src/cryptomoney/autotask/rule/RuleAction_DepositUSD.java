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
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction_DepositUSD extends Rule
{
    private double amountPerDayUSD;
    private double minimumUSDQuantityThreshold;
    private double maximumUSDQuantity;
    
    public RuleAction_DepositUSD()
    {
    }
    
    public RuleAction_DepositUSD(double _amountPerDayUSD, double _minimumUSDQuantityThreshold, double _maximumUSDQuantity)
    {
        super(RuleType.ACTION, ActionType.ACTION_WITHDRAW_BTC_TO_COINBASE);
        amountPerDayUSD = _amountPerDayUSD;
        minimumUSDQuantityThreshold = _minimumUSDQuantityThreshold;
        maximumUSDQuantity = _maximumUSDQuantity;
    }
    
    @Override
    public void doAction()
    {
        //int msPerDay = 1000*60*60*24;
        //double intervalsPerDay =  msPerDay / cbpdca.Cbpdca.iterationIntervalMS;
        //double amountPerInterval = amountPerDayUSD / intervalsPerDay;
        
        
        if(this.account.getAllowanceDepositUSD()< minimumUSDQuantityThreshold)
        {
            CryptomoneyAutotask.logProv.LogMessage("account.getAllowanceDepositUSD() does not exceed minimumUSDQuantityThreshold");
            return;
        }

        //todo: random chance of proceeding?
        //TODO: max interval of actually doing this
        
        double amountToDeposit = this.account.getAllowanceDepositUSD();
        if(amountToDeposit > maximumUSDQuantity)
        {
           amountToDeposit = maximumUSDQuantity;
        }

        List<PaymentType> paymentTypes = CryptomoneyAutotask.paymentService.getPaymentTypes();
        
        PaymentType paymentTypeBank = null;
        for(PaymentType paymentType : paymentTypes)
        {
            CryptomoneyAutotask.logProv.LogMessage("paymentType account retrieved: " + paymentType.toString()); //todo: keep this, shows last 4 digit bank#?
            if(paymentTypeBank.getCurrency().equals("USD") && paymentTypeBank.getPrimary_buy()) //TODO: abstract away USD and BTC
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
        
        
        PaymentResponse response = CryptomoneyAutotask.depositService.depositViaPaymentMethod(BigDecimal.ONE, "USD", paymentTypeBank.getId());
        CryptomoneyAutotask.logProv.LogMessage("USD deposit response: " + response.toString());
        
    }
    
    @Override
    public String getHelpString()
    {
        zz
    }
}


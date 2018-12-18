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

import cryptomoney.autotask.CryptomoneyAutotask;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class RuleAction extends Rule
{
    private double amountPerDayUSD;
    private double minimumQuantityCoinThreshold;
    private double randomChanceToProceed;
    
    public RuleAction(RuleType _ruleType, ActionType _actionType, double _amountPerDayUSD, double _minimumQuantityCoinThreshold, double _randomChanceToProceed)
    {
        super(_ruleType, _actionType);
        //ruleType = _ruleType;
        //actionType = _actionType;
        amountPerDayUSD = _amountPerDayUSD;
        minimumQuantityCoinThreshold = _minimumQuantityCoinThreshold;
        randomChanceToProceed = _randomChanceToProceed;
    }
    
    @Override
    public void DoAction()
    {
     
        //todo: could make this the chance per hour or something
        //TODO: RANDOM CHANCE TO PROCEED
        
        //int msPerDay = 1000*60*60*24;
        //double intervalsPerDay =  msPerDay / cbpdca.Cbpdca.iterationIntervalMS;
        //double amountPerInterval = amountPerDayUSD / intervalsPerDay;
        
        CryptomoneyAutotask.logProv.LogMessage("actiontype: " + actionType.toString());
        
        if(actionType == ActionType.ACTION_BUY_BTC_DCA)
        {
            double BTCPriceInUSD= 3100; //TODO: get this from a safe source, better yet two sources
            double coinAmountToPurchase = this.account.getAllowanceBuyBTCinUSD()/BTCPriceInUSD;
            
            //TODO: sanity check, don't let price be too far above 30 day average (or something).
            
            /*int maxOpenOrders = 1;
            int currentOpenOrders = 0; //TODO: get info
            if(currentOpenOrders >= maxOpenOrders)
            {
                //TODO: if that order has been open a long time, cancel it?
                return; //waiting for open order to close
            }*/
            
                    
            if(coinAmountToPurchase >= this.minimumQuantityCoinThreshold)
            {
                //todo: do this as a separate action, maybe like a separate rule
                boolean cancelAnyOpenOrders = true;
                boolean changedAllowance = this.account.ProcessBTCBuyOrders(cancelAnyOpenOrders); //API call
                if(changedAllowance) 
                {
                    coinAmountToPurchase = this.account.getAllowanceBuyBTCinUSD()/BTCPriceInUSD; //temporary, might increase amount to buy if ours was cancelled or only partially filled or something
                }
                
                //EXECUTE BUY
                CryptomoneyAutotask.logProv.LogMessage("coin amount purchase triggered: " + CryptomoneyAutotask.btcFormat.format(coinAmountToPurchase) + "/" + this.minimumQuantityCoinThreshold);      
                this.account.buyBTCPostOnly(coinAmountToPurchase); //API call
                
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("coin amount to purchase less than threshold: " + CryptomoneyAutotask.btcFormat.format(coinAmountToPurchase) + "/" + this.minimumQuantityCoinThreshold);      
            }
        }
        else
        {
            CryptomoneyAutotask.logProv.LogException(new Exception("not implemented " + actionType.toString()));
            System.exit(1);
        }
        
    }
}

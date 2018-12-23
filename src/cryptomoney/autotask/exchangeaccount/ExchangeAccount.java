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
package cryptomoney.autotask.exchangeaccount;

import com.coinbase.exchange.api.accounts.Account;
import cryptomoney.autotask.CryptomoneyAutotask;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
import cryptomoney.autotask.exchangeaccount.ExchangeType;
import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.orders.*;
import com.coinbase.exchange.api.entity.NewOrderSingle;
import com.coinbase.exchange.api.entity.Fill;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.math.MathContext;
import java.math.RoundingMode;

import cryptomoney.autotask.allowance.Allowance;
import cryptomoney.autotask.functions.SharedFunctions;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
//@Component
public class ExchangeAccount
{
    
    
    public ExchangeType exchangeType = ExchangeType.CoinbasePro;
    
    public Allowance allowanceBuyBTCinUSD = new Allowance();
    public Allowance allowanceWithdrawBTCToCoinbaseInUSD = new Allowance();
    public Allowance allowanceDepositUSD = new Allowance();
    
    private HashMap<String, Order> orders = new HashMap<>();
    
    public int btcBuyFrequencyDesperation = 0; //todo: can this be refactored, maybe make it an object
    public static final int BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD = 10;
            
    public ExchangeAccount()
    {
    }
    
    public int getKnownPendingOrderCount()
    {
        if(orders == null)
        {
            return 0;
        }
        else
        {
            return orders.size();
        }
    }
    
    /**
     * CALLS API
     * @return 
     */
    public Account getUSDCoinbaseProAccount()
    {
        //TODO: save the account ID on initial loadand retrieve just the one account instead of all
        
        
        List<Account> accounts = CryptomoneyAutotask.accountService.getAccounts();
        
        Account usdAccount = null;
        for(Account acct : accounts)
        {
            CryptomoneyAutotask.logProv.LogMessage("CPB account retrieved: " + acct.getId() + " " + acct.getCurrency() + " " + acct.getAvailable() + "/" + acct.getBalance());
            if(acct.getCurrency().equals("USD"))
            {
                if(usdAccount != null)
                {
                    CryptomoneyAutotask.logProv.LogMessage("ERROR, TWO BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                    System.exit(1);
                }
                usdAccount = acct;
            }
        }
        
        return usdAccount;
    }
    
    
    //todo: move this elsewhere
    //todo: need to test successful fills
    /**
     * CALLS API
     */
    public boolean ProcessBTCBuyOrders(boolean cancelAnyOpen) //TODO: change this to only look at buy orders
    {
        boolean madeChanges = false;
        
        if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();        
            
            List<Order> openOrders = CryptomoneyAutotask.app.orderService().getOpenOrders();
            ArrayList<Order> orphanedOrders = new ArrayList<>();
            ArrayList<Order> ordersNotOpen = new ArrayList<>();
            ArrayList<Order> ordersToCancel = new ArrayList<>();
            
            CryptomoneyAutotask.logProv.LogMessage("# open orders: " + openOrders.size());
            for(Order openOrder : openOrders)
            {
                boolean found = false;
                
                for(Order knownOrder : this.orders.values())
                {
                    if(knownOrder.getId().equals(openOrder.getId()))
                    {
                        //we know about order
                        found = true;
                        break;
                    }
                }
                
                if(!found)
                {    
                    double filledSize = Double.valueOf(openOrder.getFilled_size());
                    double originalSize = Double.valueOf(openOrder.getSize());
                    CryptomoneyAutotask.logProv.LogMessage("order ORPHAN ~pending: " + openOrder.getId() + " " + openOrder.toString() + " filled: " + filledSize + "/" + originalSize);
                    orphanedOrders.add(openOrder);
                    if(cancelAnyOpen)
                    {
                        CryptomoneyAutotask.logProv.LogMessage("Adding orphan order to cancellation queue: " + openOrder.getId());
                        ordersToCancel.add(openOrder);
                    }
                }
            }
            
            
            //see if our orders are still open
            CryptomoneyAutotask.logProv.LogMessage("checking order status...");
            for(Order knownOrder : this.orders.values())
            {
                boolean found = false;
                for(Order openOrder : openOrders)
                {
                    if(knownOrder.getId().equals(openOrder.getId()))
                    {
                        //order is still open
                        found = true;
                        if(cancelAnyOpen)
                        {
                            CryptomoneyAutotask.logProv.LogMessage("Adding known order to cancellation queue: " + openOrder.getId());
                            ordersToCancel.add(openOrder);
                        }
                    
                        double filledSize = Double.valueOf(openOrder.getFilled_size());
                        double originalSize = Double.valueOf(openOrder.getSize());
                        CryptomoneyAutotask.logProv.LogMessage("order known ~pending: " + openOrder.getId() + " " + openOrder.toString() + " filled: " + filledSize + "/" + originalSize);
                        //Cbpdca.logProv.LogMessage(openOrder.toString());
                        break;
                    }
                }
                
                if(!found)
                {    
                    ordersNotOpen.add(knownOrder);
                }
            }
            
            //CANCEL ALL OPEN ORDERS
            if(cancelAnyOpen)
            {
                if(ordersToCancel.size() > 0)
                {
                    CryptomoneyAutotask.logProv.LogMessage("cancelling orders, count: " + ordersToCancel.size());
                }
                
                for(Order orderToCancel : ordersToCancel)
                {
                    lsoc.library.utilities.Sleep.Sleep(100); //slow down so we're not querying too fast. this limits it to 10x/second

                    CryptomoneyAutotask.logProv.LogMessage("submitting cancel for " + orderToCancel.getId());
                    String response = CryptomoneyAutotask.app.orderService().cancelOrder(orderToCancel.getId()); //API CALL
                    CryptomoneyAutotask.logProv.LogMessage("Requested order cancel, response: " + response);
                    CryptomoneyAutotask.logProvFile.LogMessage("Requested order cancel, response: " + response);
                    //orders.remove(orderToCancel.getId()); //wait until later to verify it's been cancelled ? Or mark it as something we're cancelling?
                    ordersNotOpen.add(orderToCancel); //assume the cancel was complete
                    
                    this.btcBuyFrequencyDesperation++;
                    CryptomoneyAutotask.logProv.LogMessage("desperation set to: " + this.btcBuyFrequencyDesperation + "/" + this.BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD);      
                }
            }
            
            //list of orders probably closed
            CryptomoneyAutotask.logProv.LogMessage("checking open orders to mark as complete...");
            for(Order missingOrder : ordersNotOpen)
            {
                lsoc.library.utilities.Sleep.Sleep(100); //slow down so we're not querying too fast. this limits it to 10x/second
                //this sleep might also give time for any cancel requests to finish
                
                //Order actualOrder = Cbpdca.app.orderService().getOrder(missingOrder.getId());
                //if(actualOrder == null)
                //{
                CryptomoneyAutotask.logProv.LogMessage("order was not found: " + missingOrder.getId()); //maybe check fills instead

                boolean orderFound = false;
                Fill fill = null;
                
                int resultLimit = 20;
                List<Fill> fills = CryptomoneyAutotask.app.orderService().getFillByOrderId(missingOrder.getId(), resultLimit);
                
                if(fills.size() > 0)
                {
                    for(Fill fillofList : fills)
                    {
                        if(missingOrder.getId().equals(fillofList.getOrder_id()))
                        {
                            fill = fillofList;
                            break;
                        }
                    }
                    
                    if(fill != null)
                    {
                        String logString = "fill found: " + fill.getOrder_id() + " " + fill.getSize() + " " + fill.getLiquidity() + " " + fill.getProduct_id() + " " + fill.getSide() + " " + fill.getSettled();
                        CryptomoneyAutotask.logProv.LogMessage(logString); 
                        CryptomoneyAutotask.logProvFile.LogMessage(logString); 
                        orderFound = true;
                        btcBuyFrequencyDesperation=0; //stub
                    }
                }
                
                if(!orderFound)
                {
                    //we could compare ordersToCancel here
                    CryptomoneyAutotask.logProv.LogMessage("fill NOT FOUND: " + missingOrder.getId() + " assuming order was cancelled"); 
                    this.orders.remove(missingOrder.getId());
                    BigDecimal missingOrderPrice = BigDecimal.valueOf(Double.valueOf(missingOrder.getPrice()));
                    BigDecimal missingOrderCoinAmount = BigDecimal.valueOf(Double.valueOf(missingOrder.getSize()));
                    BigDecimal usdValueNotPurchased = missingOrderCoinAmount.multiply(missingOrderPrice);
                    allowanceBuyBTCinUSD.addToAllowance(usdValueNotPurchased);
                    madeChanges = true;
                    lsoc.library.utilities.Sleep.Sleep(200);
                }
                else
                {                  
                    if(fill.getSettled())
                    {
                        //double filledSize = Double.valueOf(actualOrder.getFilled_size());
                        //double originalSize = Double.valueOf(actualOrder.getSize());
                        //double missedSize = originalSize - filledSize;


                        //TODO:ASSUMING COMPLETELY FILLED??
                        double missedSize = 0;
                        
                        if(missedSize > 0)
                        {
                            //add it back to allowance
                            allowanceBuyBTCinUSD.addToAllowance(BigDecimal.valueOf(missedSize));
                            madeChanges = true;
                        }

                        this.orders.remove(missingOrder.getId()); //remove order
                        CryptomoneyAutotask.logProv.LogMessage("ORDER DONE: " + missingOrder.getId() + " " + missingOrder.toString() + " filled: " + fill.getSize().doubleValue()+ "/" + missingOrder.getSize());
                        CryptomoneyAutotask.logProvFile.LogMessage("ORDER DONE: " + missingOrder.getId() + " " + missingOrder.toString() + " filled: " + fill.getSize().doubleValue()+ "/" + missingOrder.getSize());
                        btcBuyFrequencyDesperation=0; //stub
                    }
                }
            }
        }
        else
        {
            CryptomoneyAutotask.logProv.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        return madeChanges;
    }
    
    public Order buyBTCImmediate(BigDecimal coinAmountToPurchase)
    {
         if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();
            
            
            
            CryptomoneyAutotask.logProv.LogMessage("do buy quantity: " + coinAmountToPurchase);
            
            //double purchasePrice = 3100.01; //TODO: IMPORTANT!: match one of the existing buy prices and use that.
            BigDecimal purchasePrice = SharedFunctions.GetBestBTCSellPrice(); //SELL price
            
            //TODO: make sure price isn't rediculous - like way above 24 hour avg
            
            BigDecimal sizeInBtc = coinAmountToPurchase.setScale(8, RoundingMode.HALF_EVEN);
            
            BigDecimal pricePerBtc = purchasePrice.setScale(2, RoundingMode.HALF_EVEN);
            
            Boolean post_only = false;
            String clientOid = UUID.randomUUID().toString();
            String type = "limit";
            String side = "buy";
            String product_id = "BTC-USD";
            String stp = "cb";
            String funds = "";
                    
            CryptomoneyAutotask.logProv.LogMessage("coin size rounded to: " + sizeInBtc);
            CryptomoneyAutotask.logProv.LogMessage("price rounded to: " + pricePerBtc);
            
            NewOrderSingle newOrd = new NewLimitOrderSingle(sizeInBtc, pricePerBtc, post_only, clientOid, type, side, product_id, stp, funds);
            
            Order order = CryptomoneyAutotask.app.orderService().createOrder(newOrd); //API CALL
            this.orders.put(order.getId(), order);
            CryptomoneyAutotask.logProv.LogMessage("order placed, tracking client_oid: " +  order.getId() + " " + order.toString());
            //Cbpdca.logProv.LogMessage("order details: " + order.toString());
                
            //TODO: also need to verify after it gets filled to decrement allowance
            
            //wait a short while then check order status again
            lsoc.library.utilities.Sleep.Sleep(200);
            ProcessBTCBuyOrders(false);
            
            return order;
        }
        else
        {
            CryptomoneyAutotask.logProv.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        CryptomoneyAutotask.logProv.LogException(new Exception("unexpected result in buyBTCPostOnly"));
        return null;
    }
    
    /**
     * CALLS API
     */
    public Order buyBTCPostOnly(BigDecimal coinAmountToPurchase)
    {
        if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();
            
            
            
            CryptomoneyAutotask.logProv.LogMessage("do buy quantity: " + coinAmountToPurchase);
            
            //double purchasePrice = 3100.01; //TODO: IMPORTANT!: match one of the existing buy prices and use that.
            BigDecimal purchasePrice = SharedFunctions.GetBestBTCBuyPrice();
            
            //TODO: make sure price isn't rediculous - like way above 24 hour avg
            
            BigDecimal sizeInBtc = coinAmountToPurchase.setScale(8, RoundingMode.HALF_EVEN);
            
            BigDecimal pricePerBtc = purchasePrice.setScale(2, RoundingMode.HALF_EVEN);
            
            Boolean post_only = true; //note: this makes it so it won't execute the order immediately (might even cancel it), instead go to order book.  No fees for this!
            String clientOid = UUID.randomUUID().toString();
            String type = "limit";
            String side = "buy";
            String product_id = "BTC-USD";
            String stp = "cb";
            String funds = "";
                    
            CryptomoneyAutotask.logProv.LogMessage("coin size rounded to: " + sizeInBtc);
            CryptomoneyAutotask.logProv.LogMessage("price rounded to: " + pricePerBtc);
            
            NewOrderSingle newOrd = new NewLimitOrderSingle(sizeInBtc, pricePerBtc, post_only, clientOid, type, side, product_id, stp, funds);
            
            if(newOrd == null)
            {
                CryptomoneyAutotask.logProv.LogException(new Exception("NewOrderSingle null, shouldn't be!"));
                CryptomoneyAutotask.logProvFile.LogException(new Exception("NewOrderSingle null, shouldn't be!"));
                return null;
            }
            
            Order order = CryptomoneyAutotask.app.orderService().createOrder(newOrd); //API CALL
            
            if(order == null)
            {
                String logInfo = "order respons is null, order probably failed";
                CryptomoneyAutotask.logProv.LogMessage(logInfo);
                CryptomoneyAutotask.logProvFile.LogMessage(logInfo);
                return null;
            }
            
            this.orders.put(order.getId(), order);
            CryptomoneyAutotask.logProv.LogMessage("order placed, tracking client_oid: " +  order.getId() + " " + order.toString());
            //Cbpdca.logProv.LogMessage("order details: " + order.toString());
                
            //TODO: also need to verify after it gets filled to decrement allowance
            
            //wait a short while then check order status again
            lsoc.library.utilities.Sleep.Sleep(200);
            ProcessBTCBuyOrders(false);
            
            return order;
        }
        else
        {
            CryptomoneyAutotask.logProv.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        CryptomoneyAutotask.logProv.LogException(new Exception("unexpected result in buyBTCPostOnly"));
        return null;
    }




}

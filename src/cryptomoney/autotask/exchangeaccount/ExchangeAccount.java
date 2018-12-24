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


import cryptomoney.autotask.CryptomoneyAutotask;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
import cryptomoney.autotask.exchangeaccount.ExchangeType;
import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.orders.*;
import com.coinbase.exchange.api.entity.NewOrderSingle;
import com.coinbase.exchange.api.entity.Fill;
import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.payments.CoinbaseAccount;
import com.coinbase.exchange.api.payments.PaymentType;
import static cryptomoney.autotask.CryptomoneyAutotask.logMultiplexer;
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
    
    private String coinbaseProUSDAccountId = null;
    private String coinbaseProUSDBankPaymentTypeId = null;
    private String coinbaseProBTCAccountId = null;
    private String coinbaseRegularBTCAccountId = null;
    
    private boolean has_coinbaseProUSDAccountId = false;
    private boolean has_coinbaseProUSDBankPaymentTypeId = false;
    private boolean has_coinbaseProBTCAccountId = false;
    private boolean has_coinbaseRegularBTCAccountId = false;
            
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
     * This is needed for informational purposes only.  In most cases, it's only necessary to get the account_id.
     * @return 
     */
    public CoinbaseAccount getCoinbaseRegularBTCAccountById(String _accountId)
    {
        List<CoinbaseAccount> coinbaseAccounts = CryptomoneyAutotask.paymentService.getCoinbaseAccounts(); //optional: instead of this get the id somehow else and code it into config?
        CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase accounts, count: "+coinbaseAccounts.size());

        CoinbaseAccount btcCoinbaseAccount = null;
        for(CoinbaseAccount coinbaseAccount : coinbaseAccounts)
        {
            CryptomoneyAutotask.logProv.LogMessage("coinbase account retrieved: " + coinbaseAccount.getId() + " " + 
                    coinbaseAccount.getCurrency() + " " + 
                    coinbaseAccount.getType() + " " + 
                    coinbaseAccount.getPrimary() + " " + 
                    coinbaseAccount.getBalance() + " " + 
                    coinbaseAccount.getName()
                    );
            if(coinbaseAccount.getId().equals(_accountId))
            {
                if(btcCoinbaseAccount != null)
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO -PRIMARY- BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                        //in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                    System.exit(1);
                }
                btcCoinbaseAccount = coinbaseAccount;
            }
        }

        if(btcCoinbaseAccount != null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get coinbaseRegularBTCAccount. Exiting!");
            System.exit(1);
        }
        else
        {
            return btcCoinbaseAccount;
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getCoinbaseRegularBTCAccountById. Exiting!");
        System.exit(1);
        return null; 
    }
    
    /**
     * API CALL
     * @return 
     */
    public String getCoinbaseRegularBTCAccount_Id()
    {
        if(coinbaseRegularBTCAccountId != null)
        {
            return coinbaseRegularBTCAccountId;
        }
        else
        {
            List<CoinbaseAccount> coinbaseAccounts = CryptomoneyAutotask.paymentService.getCoinbaseAccounts(); //optional: instead of this get the id somehow else and code it into config?
            CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase accounts, count: "+coinbaseAccounts.size());

            CoinbaseAccount btcCoinbaseAccount = null;
            for(CoinbaseAccount coinbaseAccount : coinbaseAccounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("coinbase account retrieved: " + coinbaseAccount.getId() + " " + 
                        coinbaseAccount.getCurrency() + " " + 
                        coinbaseAccount.getType() + " " + 
                        coinbaseAccount.getPrimary() + " " + 
                        coinbaseAccount.getBalance() + " " + 
                        coinbaseAccount.getName()
                        );
                if(coinbaseAccount.getCurrency().equals("BTC") && coinbaseAccount.getPrimary())
                {
                    if(btcCoinbaseAccount != null)
                    {
                        CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO -PRIMARY- BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                            //in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                        System.exit(1);
                    }
                    btcCoinbaseAccount = coinbaseAccount;
                }
            }
            
            if(btcCoinbaseAccount == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get coinbaseRegularBTCAccount_Id. Exiting!");
                System.exit(1);
            }
            else
            {
                this.coinbaseRegularBTCAccountId = btcCoinbaseAccount.getId();
                this.has_coinbaseRegularBTCAccountId = true;
                return coinbaseRegularBTCAccountId;
            }
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getCoinbaseRegularBTCAccount_Id. Exiting!");
        System.exit(1);
        return null;        
    }
    
    public Account getCoinbaseProBTCAccount()
    {
        if(coinbaseProBTCAccountId != null)
        {
            Account acct = CryptomoneyAutotask.accountService.getAccount(coinbaseProBTCAccountId);
            if(acct == null)
            {
                this.coinbaseProBTCAccountId = null; //reset, account wasn't found
            }
            return acct;
        }
        else
        {
            List<Account> accounts = CryptomoneyAutotask.accountService.getAccounts();
            CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase PRO accounts, count: "+accounts.size());

            Account btcAccount = null;
            for(Account acct : accounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("CPB account retrieved: " + acct.getId() + " " + acct.getCurrency() + " " + acct.getAvailable() + "/" + acct.getBalance());
                if(acct.getCurrency().equals("BTC"))
                {
                    if(btcAccount != null)
                    {
                        CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO BTC ACCOUNTS FOUND WHEN EXPECTING ONE, EXITING"); //todo: test this to make sure there would only be one account
                        System.exit(1);
                    }
                    btcAccount = acct;
                }
            }
            
            if(btcAccount == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get getCoinbaseProBTCAccount. Exiting!");
                System.exit(1);
            }
            else
            {
                this.coinbaseProBTCAccountId = btcAccount.getId();
                this.has_coinbaseProBTCAccountId = true;
                return btcAccount;
            }
        }
                
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getCoinbaseProBTCAccount. Exiting!");
        System.exit(1);
        return null;
    }
    
    /**
     * get unique id representing bank account used to fund coinbase pro
     * Sometimes API call
     * @return 
     */
    public String getCoinbaseProUSDBankPaymentType_Id()
    {
        if(coinbaseProUSDBankPaymentTypeId != null)
        {
            return this.coinbaseProUSDBankPaymentTypeId; //only this info is needed usually
        }
        else
        {

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
                        System.exit(1);
                    }
                    paymentTypeBank = paymentType;
                }
            }


            if(paymentTypeBank == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get getCoinbaseProUSDBankPaymentType_Id. Exiting!");
                System.exit(1);
            }
            else
            {
                this.coinbaseProUSDBankPaymentTypeId = paymentTypeBank.getId();
                this.has_coinbaseProUSDBankPaymentTypeId = true;
                return paymentTypeBank.getId();
            }
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation coinbaseProUSDAccount. Exiting!");
        System.exit(1);
        return null;
    }
    
    /**
     * If we know the account ID, retrieve just that one, otherwise retrieve all, find it, and save the account ID for later use.
     * CALLS API
     * @return 
     */
    public Account getCoinbaseProUSDAccount()
    {
        if(coinbaseProUSDAccountId != null)
        {
            Account acct = CryptomoneyAutotask.accountService.getAccount(this.coinbaseProUSDAccountId);
            if(acct == null)
            {
                this.coinbaseProUSDAccountId = null; //reset, account wasn't found
            }
            return acct;
        }
        else
        {
            List<Account> accounts = CryptomoneyAutotask.accountService.getAccounts();

            Account usdAccount = null;
            for(Account acct : accounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("CPB account retrieved: " + acct.getId() + " " + acct.getCurrency() + " " + acct.getAvailable() + "/" + acct.getBalance());
                if(acct.getCurrency().equals("USD"))
                {
                    if(usdAccount != null)
                    {
                        CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO BTC ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                        System.exit(1);
                    }
                    usdAccount = acct;
                }
            }

            if(usdAccount == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get coinbaseProUSDAccount. Exiting!");
                System.exit(1);
            }
            else
            {
                this.coinbaseProUSDAccountId = usdAccount.getId();
                this.has_coinbaseProUSDAccountId = true;
                return usdAccount;
            }
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getUSDCoinbaseProAccount. Exiting!");
        System.exit(1);
        return null;
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
                
                    for(Order orderToCancel : ordersToCancel)
                    {
                        lsoc.library.utilities.Sleep.Sleep(100); //slow down so we're not querying too fast. this limits it to 10x/second

                        CryptomoneyAutotask.logProv.LogMessage("submitting cancel for " + orderToCancel.getId());
                        String response = CryptomoneyAutotask.app.orderService().cancelOrder(orderToCancel.getId()); //API CALL
                        CryptomoneyAutotask.logMultiplexer.LogMessage("Requested order cancel, response: " + response);
                        //orders.remove(orderToCancel.getId()); //wait until later to verify it's been cancelled ? Or mark it as something we're cancelling?
                        ordersNotOpen.add(orderToCancel); //assume the cancel was complete

                        this.btcBuyFrequencyDesperation++;
                        CryptomoneyAutotask.logProv.LogMessage("desperation set to: " + this.btcBuyFrequencyDesperation + "/" + this.BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD);      
                    }
                }
            }
            
            //list of orders probably closed
            if(ordersNotOpen.size() > 0)
            {
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
                            CryptomoneyAutotask.logMultiplexer.LogMessage(logString); 
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
                            CryptomoneyAutotask.logMultiplexer.LogMessage("ORDER DONE: " + missingOrder.getId() + " " + missingOrder.toString() + " filled: " + fill.getSize().doubleValue()+ "/" + missingOrder.getSize());
                            btcBuyFrequencyDesperation=0; //stub
                        }
                    }
                }
            }
        }
        else
        {
            CryptomoneyAutotask.logMultiplexer.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
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
            CryptomoneyAutotask.logMultiplexer.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        CryptomoneyAutotask.logMultiplexer.LogException(new Exception("unexpected result in buyBTCPostOnly"));
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
                CryptomoneyAutotask.logMultiplexer.LogException(new Exception("NewOrderSingle null, shouldn't be!"));
                return null;
            }
            
            Order order = CryptomoneyAutotask.app.orderService().createOrder(newOrd); //API CALL
            
            if(order == null)
            {
                String logInfo = "order respons is null, order probably failed";
                CryptomoneyAutotask.logMultiplexer.LogMessage(logInfo);
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
            CryptomoneyAutotask.logMultiplexer.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        CryptomoneyAutotask.logMultiplexer.LogException(new Exception("unexpected result in buyBTCPostOnly"));
        return null;
    }

    /**
     * @return the has_coinbaseProUSDAccountId
     */
    public boolean isHas_coinbaseProUSDAccountId()
    {
        return has_coinbaseProUSDAccountId;
    }

    /**
     * @return the has_coinbaseProUSDBankPaymentTypeId
     */
    public boolean isHas_coinbaseProUSDBankPaymentTypeId()
    {
        return has_coinbaseProUSDBankPaymentTypeId;
    }

    /**
     * @return the has_coinbaseProBTCAccountId
     */
    public boolean isHas_coinbaseProBTCAccountId()
    {
        return has_coinbaseProBTCAccountId;
    }

    /**
     * @return the has_coinbaseRegularBTCAccountId
     */
    public boolean isHas_coinbaseRegularBTCAccountId()
    {
        return has_coinbaseRegularBTCAccountId;
    }




}

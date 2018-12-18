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
import java.math.BigDecimal;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
//@Component
public class ExchangeAccount
{
    
    
    public ExchangeType exchangeType = ExchangeType.CoinbasePro;
    
    private double allowanceBuyBTCinUSD = 0;
    
    private HashMap<String, Order> orders = new HashMap<>();
    
            
    public ExchangeAccount()
    {
    }
    
    public void addAllowanceBuyBTCinUSD(double _inc)
    {
        allowanceBuyBTCinUSD+=_inc;
    }

    /**
     * @return the allowanceBuyBTC
     */
    public double getAllowanceBuyBTCinUSD()
    {
        return allowanceBuyBTCinUSD;
    }
    
    
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
                            ordersToCancel.add(openOrder);
                        }
                    
                        double filledSize = Double.valueOf(openOrder.getFilled_size());
                        double originalSize = Double.valueOf(openOrder.getSize());
                        CryptomoneyAutotask.logProv.LogMessage("order ~pending: " + openOrder.getId() + " " + openOrder.toString() + " filled: " + filledSize + "/" + originalSize);
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
                    CryptomoneyAutotask.logProv.LogMessage("cancelling open orders...");
                }
                
                for(Order orderToCancel : ordersToCancel)
                {
                    lsoc.library.utilities.Sleep.Sleep(100); //slow down so we're not querying too fast. this limits it to 10x/second

                    CryptomoneyAutotask.logProv.LogMessage("submitting cancel for " + orderToCancel.getId());
                    CryptomoneyAutotask.app.orderService().cancelOrder(orderToCancel.getId());
                    //orders.remove(orderToCancel.getId()); //wait until later to verify it's been cancelled ? Or mark it as something we're cancelling?
                    ordersNotOpen.add(orderToCancel); //assume the cancel was complete
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
                List<Fill> fills = CryptomoneyAutotask.app.orderService().getFillByOrderId(Integer.valueOf(missingOrder.getId()), resultLimit);
                
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
                        CryptomoneyAutotask.logProv.LogMessage("debug: fill found: " + fill.getOrder_id() + " " + fill.toString() + " " + fill.getSettled()); 
                        orderFound = true;
                    }
                }
                
                if(!orderFound)
                {
                    //we could compare ordersToCancel here
                    CryptomoneyAutotask.logProv.LogMessage("fill NOT FOUND: " + missingOrder.getId() + " assuming order was cancelled"); 
                    this.orders.remove(missingOrder.getId());
                    allowanceBuyBTCinUSD+= Double.valueOf(missingOrder.getSize())*Double.valueOf(missingOrder.getPrice());
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
                            allowanceBuyBTCinUSD+=missedSize;
                            madeChanges = true;
                        }

                        this.orders.remove(missingOrder.getId()); //remove order
                        CryptomoneyAutotask.logProv.LogMessage("ORDER DONE: " + missingOrder.getId() + " " + missingOrder.toString() + " filled: ?"  + "/?" );

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
    
    /**
     * CALLS API
     */
    public void buyBTCPostOnly(double coinAmountToPurchase)
    {
        if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();
            
            
            
            CryptomoneyAutotask.logProv.LogMessage("do buy quantity: " + coinAmountToPurchase);
            
            double purchasePrice = 3100.01; //TODO: IMPORTANT!: match one of the existing buy prices and use that.
            
            
            BigDecimal sizeInBtc = BigDecimal.valueOf(coinAmountToPurchase).setScale(8, RoundingMode.HALF_EVEN);
            
            BigDecimal pricePerBtc = BigDecimal.valueOf(purchasePrice).setScale(2, RoundingMode.HALF_EVEN);
            
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
            
            
            /*
            OrderBuilder newOrdBuild = new OrderBuilder().setId(clientOid).setSide(side).setProduct_id(product_id).setSize(sizeInBtc.toString()).setPrice(pricePerBtc.toString());
            Order newOrder = newOrdBuild.build();
            newOrder.setPost_only(post_only.toString());
            newOrder.setType(type);
            newOrder.setStp(stp);
            */          
                    
            Order order = CryptomoneyAutotask.app.orderService().createOrder(newOrd); //this places order!
            this.orders.put(order.getId(), order);
            CryptomoneyAutotask.logProv.LogMessage("order placed, tracking client_oid: " +  order.getId() + " " + order.toString());
            //Cbpdca.logProv.LogMessage("order details: " + order.toString());
                
            double estimatedUSDToSpend = coinAmountToPurchase*purchasePrice;
            this.allowanceBuyBTCinUSD-=estimatedUSDToSpend;
            //TODO: also need to verify after it gets filled to decrement allowance
            
            //wait a short while then check order status again
            lsoc.library.utilities.Sleep.Sleep(200);
            ProcessBTCBuyOrders(false);
            
            
        }
        else
        {
            CryptomoneyAutotask.logProv.LogException(new Exception("not implemented buyBTC " + this.exchangeType.toString()));
            System.exit(1);
        }
    }
    


}

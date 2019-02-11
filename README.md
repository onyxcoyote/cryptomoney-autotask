A program used to automate tasks on Coinbase Pro, using the Coinbase Pro API.

see LICENSE.txt

Examples:
1) automatically buy certain cryptocurrency coins in specific quantities twice per day
2) automatically deposit fiat funds, buy cryptocurrencies, and withdraw cryptocurrencies to coinbase (e.g. to automatically set a budget for a Debit Card that can connect to Coinbase, by automatically addings funds available to it)


How to compile:
1) compile gdax-java as a library, it is not necessary to input API keys into gdax-java (NOTE: this program currently uses a version of gdax-java that does not exist yet, therefore it is not possible to compile this program without making some changes to gdax-java)
2) compile lsoclibrary
3) compile this program


How to use:
1) input api keys and api URL into a properties file.  DO NOT CHECK THAT FILE IN.  To prevent checking the file in, keep that file outside of the repository.
 note: the property execute_immediately, if set to true, to execute 1 day's worth of tasks immediately
2) update properties files with the desired tasks.  Up to 20 versions of each task can be added.  Examples:
ACTION_BUY_COIN_DCA_POSTONLY_1_*
ACTION_BUY_COIN_DCA_POSTONLY_2_*
3) update metaconfig.properties to point to your new properties file
4) to set up program as a service on a CentOS server, there is an example service file included as cryptomoney-autotask-service 
#copy cryptomoney-autotask-service to /etc/init.d/
#copy the compiled program code, library files in lib/ folder, and properties files, to the desired location, e.g. /srv/cryptomoney-autotask/
#enable autostart (one-time only):
chkconfig cryptomoney-autotask-service on
#to start it
service cryptomoney-autotask-service start
#to stop it
service cryptomoney-autotask-service stop
#to check status
service cryptomoney-autotask-service status
#to restart
service cryptomoney-autotask-service restart


The available tasks are:
1) ACTION_BUY_COIN_DCA_POSTONLY_* - buys a specific cryptocurrency in specific amounts and frequencies.  Uses dollar-cost-averaging, and attempts to use post-only if possible which (as of today) has no fees.  If the trade is not successful after a period of time, it will make an immediate market trade, which does (as of today) have a fee.
2) ACTION_WITHDRAW_COIN_TO_COINBASE_* - withdraws a type of cryptocurrency from CoinbasePro to Coinbase, in specific amounts and frequencies
3) ACTION_DEPOSIT_FIAT_* - deposit fiat currency from a bank account to CoinbasePro
4) ALARM_PRINT_BALANCE_* - prints relevant account balance
5) ALARM_PRINT_BALANCE_INCLUDE_* - include specific not relevant account balances in the account balance output
6) ALLOWANCE ALLOWANCE_WITHDRAW_COIN_TO_CRYPTO_* - withdraws a type of cryptocurrency to a specified crypto wallet, given a minimum balance in coinbasePro of that cryptocurrency


 

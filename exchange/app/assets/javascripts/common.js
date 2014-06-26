// global configurations and constant objects

COINPORT = {
    priceFixed: {
        'btc-cny': 2,
        'ltc-btc': 4,
        'doge-btc': 8
    },

    amountFixed: {
        cny: 4,
        btc: 4,
        ltc: 2,
        doge: 2
    },

    defaultMarket: 'LTCBTC',

    blockUrl: {
        BTC: 'https://blockchain.info/block-index/',
        LTC: 'http://block-explorer.com/block/',
        DOGE: 'http://dogechain.info/block/'
    },

    addressUrl: {
        BTC: 'https://blockchain.info/address/',
        LTC: 'http://block-explorer.com/address/',
        DOGE: 'http://dogechain.info/address/'
    },

    txUrl: {
        BTC: 'https://blockchain.info/tx/',
        LTC: 'http://block-explorer.com/tx/',
        DOGE: 'http://dogechain.info/tx/'
    }
};

COINPORT.getPriceFixed = function(market) {
    return COINPORT.priceFixed[market.toLowerCase()];
};

COINPORT.getAmountFixed = function(currency) {
    return COINPORT.amountFixed[currency.toLowerCase()];
};
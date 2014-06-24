// global configurations and constant objects

COINPORT = {
    priceFixed: {
        btccny: 2,
        ltcbtc: 4,
        dogbtc: 8
    },

    amountFixed: {
        cny: 4,
        btc: 4,
        ltc: 2,
        dog: 2
    },

    defaultMarket: 'LTCBTC',

    blockUrl: {
        BTC: 'https://blockchain.info/block-index/',
        LTC: 'http://block-explorer.com/block/',
        DOG: 'http://dogechain.info/block/'
    },

    addressUrl: {
        BTC: 'https://blockchain.info/address/',
        LTC: 'http://block-explorer.com/address/',
        DOG: 'http://dogechain.info/address/'
    },

    txUrl: {
        BTC: 'https://blockchain.info/tx/',
        LTC: 'http://block-explorer.com/tx/',
        DOG: 'http://dogechain.info/tx/'
    }
};

COINPORT.getPriceFixed = function(market) {
    return COINPORT.priceFixed[market.toLowerCase()];
};

COINPORT.getAmountFixed = function(currency) {
    return COINPORT.amountFixed[currency.toLowerCase()];
};
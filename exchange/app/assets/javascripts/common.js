// global configurations and constant objects

COINPORT = {
    priceFixed: {
        'btc-cny': 2,
        'ltc-btc': 4,
        'doge-btc': 8,
        'bc-btc': 8,
        'drk-btc': 6
    },

    amountFixed: {
        cny: 4,
        btc: 4,
        ltc: 2,
        doge: 2,
        bc: 3,
        drk: 2
    },

    defaultMarket: 'LTC-BTC',

    blockUrl: {
        BTC: 'https://blockchain.info/block-index/',
        LTC: 'http://block-explorer.com/block/',
        DOGE: 'http://dogechain.info/block/',
        BC: '#',
        DRK: '#'
    },

    addressUrl: {
        BTC: 'https://blockchain.info/address/',
        LTC: 'http://block-explorer.com/address/',
        DOGE: 'http://dogechain.info/address/',
        BC: '#',
        DRK: '#'
    },

    txUrl: {
        BTC: 'https://blockchain.info/tx/',
        LTC: 'http://block-explorer.com/tx/',
        DOGE: 'http://dogechain.info/tx/',
        BC: '#',
        DRK: '#'
    }
};

COINPORT.getPriceFixed = function(market) {
    return COINPORT.priceFixed[market.toLowerCase()];
};

COINPORT.getAmountFixed = function(currency) {
    return COINPORT.amountFixed[currency.toLowerCase()];
};
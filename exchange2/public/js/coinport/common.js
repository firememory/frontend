// global configurations and constant objects

COINPORT = {
    priceFixed: {
        'btc-cny': 2,
        'ltc-cny': 2,
        'btsx-cny': 4,
        'xrp-cny': 4,
        'ltc-btc': 4,
        'doge-btc': 8,
        'bc-btc': 8,
        'drk-btc': 6,
        'vrc-btc': 8,
        'zet-btc': 8,
        'btsx-btc': 8,
        'nxt-btc': 8,
        'xrp-btc': 8
    },

    amountFixed: {
        cny: 3,
        btc: 8,
        ltc: 8,
        doge: 8,
        bc: 8,
        drk: 8,
        vrc: 8,
        zet: 8,
        btsx: 8,
        nxt: 8,
        xrp: 8
    },

//    amountFixed: {
//        cny: 4,
//        btc: 4,
//        ltc: 4,
//        doge: 4,
//        bc: 3,
//        drk: 2,
//        vrc: 3,
//        zet: 3
//    },

    defaultMarket: 'BTC-CNY',

    blockUrl: {
        BTC: 'https://blockchain.info/block-index/',
        LTC: 'http://block-explorer.com/block/',
        DOGE: 'http://dogechain.info/block/',
        BC: 'http://blackcha.in/block/',
        DRK: 'http://explorer.darkcoin.io/block/',
        VRC: 'https://chainz.cryptoid.info/vrc/block.dws?', //'http://blocks.vericoin.info/block/',
        ZET: 'https://coinplorer.com/ZET/Blocks/',
        BTSX: 'http://blockchain.bitsuperlab.com',
        XRP: 'https://ripple.com/developers/',
        NXT: 'http://87.230.14.1/nxt/nxt.cgi?action=1000&blk='
    },

    addressUrl: {
        BTC: 'https://blockchain.info/address/',
        LTC: 'http://block-explorer.com/address/',
        DOGE: 'http://dogechain.info/address/',
        BC: 'http://blackcha.in/address/',
        DRK: 'http://explorer.darkcoin.io/address/',
        VRC: 'https://chainz.cryptoid.info/vrc/address.dws?', //'http://blocks.vericoin.info/address/',
        ZET: 'https://coinplorer.com/ZET/Addresses/',
        BTSX: 'http://blockchain.bitsuperlab.com',
        NXT: 'http://87.230.14.1/nxt/nxt.cgi?action=3000&acc='
    },

    txUrl: {
        BTC: 'https://blockchain.info/tx/',
        LTC: 'http://block-explorer.com/tx/',
        DOGE: 'http://dogechain.info/tx/',
        BC: 'http://blackcha.in/tx/',
        DRK: 'http://explorer.darkcoin.io/tx/',
        VRC: 'https://chainz.cryptoid.info/vrc/tx.dws?', //'http://blocks.vericoin.info/tx/',
        ZET: 'https://coinplorer.com/ZET/Transactions/',
        BTSX: 'http://blockchain.bitsuperlab.com',
        NXT: 'http://87.230.14.1/nxt/nxt.cgi?action=2000&tra='
    }
};

COINPORT.getPriceFixed = function(market) {
    return COINPORT.priceFixed[market.toLowerCase()];
};

COINPORT.getAmountFixed = function(currency) {
    return COINPORT.amountFixed[currency.toLowerCase()];
};

COINPORT.floor = function(value, precision) {
    if (isNaN(value))
        return value;

    var s = '' + value;
    var offset = s.indexOf('.');
    if (offset < 0)
        return value;
    offset += precision;
    return s.substring(0, offset + 1);
};

COINPORT.numberRegExp =  /^\d*(\.)?(\d+)*$/;

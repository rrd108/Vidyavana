
var load = require('./load');
var util = require('./util');

/** @type {Search} */
var search, pendingSearch;

/** @type {boolean} - if a search-related message is visible */
var searchMsgShown;

function Search()
{
    /**
     * @type {string} - originally entered text
     */
    var query;
    /**
     * @type {string} - originally selected order
     */
    var _sort;
    /**
     * @type {SearchResponse} - details of last hit shown
     */
    var last;

    /**
     * @param {string?} q
     * @return {string}
     */
    function queryFn(q) {
        if(q === undefined)
            return query;
        query = q;
    }

    /**
     * @param {string?} sort
     * @return {string}
     */
    function sortFn(sort) {
        if(sort === undefined)
            return _sort;
        _sort = sort;
    }


    /**
     * @param {SearchResponse?} l
     * @return {SearchResponse}
     */
    function lastFn(l) {
        if(l === undefined)
            return last;
        last = l;
    }

    this.query = queryFn;
    this.sort = sortFn;
    this.last = lastFn;
}


function accept() {
    search = pendingSearch;
}


/**
 * One-time setup of event handlers.
 */
function init()
{
    var $inp = $('#searchInput');
    /** @type {JQuery|Array.<HTMLInputElement>} */
    var $scoreOrder = $('#score-order');
    $inp.keydown(function(e)
    {
        if(searchMsgShown)
        {
            $('#search-msg').hide();
            searchMsgShown = false;
        }
        if(e.keyCode == 13)
        {
            newSearch($inp.val(), $scoreOrder[0].checked);
        }
        if(!util.menuModifier(e))
            //noinspection JSUnresolvedFunction
            e.stopPropagation();
    });
    $('#searchGo').click(function()
    {
        newSearch($inp.val(), $scoreOrder[0].checked);
    });
}


function newSearch(text, scoreOrder)
{
    if(searchMsgShown)
    {
        $('#search-msg').hide();
        searchMsgShown = false;
    }
    var ps = pendingSearch = new Search();
    ps.query(text);
    ps.sort(scoreOrder ? 'Score' : 'Index');
    load.text(load.mode.search);
}


function message(msg) {
    $('#search-msg').text(msg).show();
    searchMsgShown = true;

}

/**
 * @param {boolean?} pending
 * @return {Search}
 */
function getInstance(pending) {
    return pending ? pendingSearch : search;
}


$.extend(exports, {
    get: getInstance,
    pending: getInstance.bind(null, true),
    init: init,
    accept: accept,
    message: message
});

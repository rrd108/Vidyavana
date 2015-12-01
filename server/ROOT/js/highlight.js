var LOWER = "áéíóöőúüűāīūḍḥḷḹṁṅṇñṛṝṣśṭ";
var UPPER = "ÁÉÍÓÖŐÚÜŰĀĪŪḌḤḶḸṀṄṆÑṚṜṢŚṬ";
var TRANS = "āīūḍḥḷḹṁṅṇñṛṝṣśṭ";
var PLAIN = "aiudhllmnnnrrsst";

var lowerMap = {};
for(var i=0; i<LOWER.length; ++i)
{
    var low = LOWER.charAt(i);
    lowerMap[low] = low;
    lowerMap[UPPER.charAt(i)] = low;
}

var plainMap = {};
for(i=0; i<TRANS.length; ++i)
    plainMap[TRANS.charAt(i)] = PLAIN.charAt(i);



/**
 * @constructor
 * @param {string} queryStr - current search term
 */
function Highlight(queryStr)
{
    var paraRex = /<p.*?>(.*?)<\/p>/g;
    var htmlRex = /(.*?)(<.*?>|&.*;|$)/g;
    var whiteRex = /(.*?)( |$)/g;

    var q = lowercase(queryStr);
    var soughtArrArr = words(q);
    for(var i in soughtArrArr)
    {
        var soughtArr = soughtArrArr[i];
        var word = soughtArr[0];
        // if entered word has translit, take it literally
        if(plain(word) !== word)
            soughtArr[1] = true;
    }


    function lowercase(s)
    {
        var t = '';
        for(var i=0; i<s.length; ++i)
            t += lowerChar(s.charAt(i));
        return t;
    }


    function lowerChar(c)
    {
        if(c >= 'a' && c <= 'z' || c >= '0' && c <= '9');
        else
        {
            var ch = lowerMap[c];
            if(ch)
                c = ch;
            else
                c = c.toLowerCase();
        }
        return c;
    }


    function plain(s)
    {
        var t = '';
        for(var i=0; i<s.length; ++i)
        {
            var c = s.charAt(i);
            var ch = plainMap[c];
            if(ch)
                c = ch;
            t += c;
        }
        return t;
    }


    /**
     * @param {string} s - lowercased string of possibly multiple words
     * @return {Array.<Array>} - array of words from string as [word, start, end], where indexes are relative to q
     */
    function words(s)
    {
        var res = [];
        var inWord = false;
        var word = '';
        var start;
        for(var i = 0, len = s.length; i < len; ++i)
        {
            var c = s.charAt(i);
            var wc = isLowerWordChar(c);
            if(!inWord && wc)
            {
                inWord = true;
                start = i;
            }
            else if(inWord && !wc)
            {
                inWord = false;
                if(word)
                {
                    res.push([word, start, i]);
                    word = '';
                }
            }
            if(inWord)
                word += c;
        }
        if(inWord)
            res.push([word, start, len]);
        return res;
    }


    function isLowerWordChar(c)
    {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || !!lowerMap[c];
    }


    /**
     * Split html into paragraphs and invoke highlighting in each.
     * @param {string} text - html chunk
     */
    function run(text)
    {
        paraRex.lastIndex = 0;
        nextPara(text);
    }


    function nextPara(text)
    {
        var res = paraRex.exec(text);
        if(!res)
            return;
        setTimeout(paraText.bind(this, text, res[0], res[1]), 1);
    }


    function paraText(text, pText, pContent)
    {
        var highlightIxPairArr = highlightIndexes(pContent);
        if(highlightIxPairArr.length > 0)
        {
            var res = /data-ix="(\d+)"/.exec(pText);
            if(res)
            {
                var paraId = res[1];
                var chunks = [];
                var pos = 0;
                for(var i in highlightIxPairArr)
                {
                    var ixPair = highlightIxPairArr[i];
                    if(pos < ixPair[0])
                        chunks.push(pContent.substring(pos, ixPair[0]));
                    chunks.push('<span class="hilite">', pContent.substring(ixPair[0], ixPair[1]), '</span>');
                    pos = ixPair[1];
                }
                if(pos < pContent.length)
                    chunks.push(pContent.substring(pos));
                $('p[data-ix="'+paraId+'"]', $txt).html(chunks.join(''));
            }
        }
        nextPara(text);
    }


    function highlightIndexes(pContent)
    {
        var ret = [];
        htmlRex.lastIndex = 0;
        while(true)
        {
            var htmlStart = htmlRex.lastIndex;
            var res = htmlRex.exec(pContent);
            if(!res || !res[0])
                break;
            if(!res[1])
                continue;
            var toNextTag = res[1];
            whiteRex.lastIndex = 0;
            while(true)
            {
                var whiteStart = htmlStart + whiteRex.lastIndex;
                res = whiteRex.exec(toNextTag);
                if(!res || !res[0])
                    break;
                if(!res[1])
                    continue;
                var toNextWhite = res[1];
                var q = lowercase(toNextWhite);
                var whiteWordArrArr = words(q);
                for(var i in whiteWordArrArr)
                {
                    var whiteWordArr = whiteWordArrArr[i];
                    if(sought(whiteWordArr))
                        ret.push([whiteStart+whiteWordArr[1], whiteStart+whiteWordArr[2]]);
                }
            }
        }
        return ret;
    }


    function sought(wordArr)
    {
        for(var i in soughtArrArr)
        {
            var soughtArr = soughtArrArr[i];
            var word = soughtArr[0];
            var strict = soughtArr[1];
            if(strict)
            {
                if(word === wordArr[0])
                    return true;
            }
            else if(word === plain(wordArr[0]))
                return true;
        }
        return false;
    }


    $.extend(this, {
        run: run,
        // for tests
        wordArr: function(saa) {soughtArrArr = saa;},
        lowercase: lowercase,
        words: words,
        highlightIndexes: highlightIndexes,
        sought: sought
    });
}

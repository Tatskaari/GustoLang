define("ace/mode/gusto_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

var AdaHighlightRules = function() {
var keywords = "else|return|end|do|of|type|for|" +
               "function|begin|val|then|if|type|match|with";
    var builtinConstants = (
        "true|false"
    );

    var builtinFunctions = (
        "size|output|input|random"
    );

    var keywordMapper = this.createKeywordMapper({
        "support.function": builtinFunctions,
        "keyword": keywords,
        "constant.language": builtinConstants
    }, "identifier", true);

    this.$rules = {
        "start" : [ {
            token : "comment",
            regex : "\\(\\*.*\\*\\)"
        }, {
            token : "string",           // " string
            regex : '".*?"'
        }, {
            token : "constant.numeric", // float
            regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
        }, {
            token : "variable.parameter",
            regex : "[A-Z][a-zA-Z0-9_']*"
        }, {
            token : keywordMapper,
            regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
        }, {
            token : "keyword.operator",
            regex : "\\+|\\-|\\/|\\/\\/|%|<@>|@>|<@|&|\\^|~|<|>|<=|=>|==|!=|<>|=|:"
        }, {
            token : "paren.lparen",
            regex : "[\\(]"
        }, {
            token : "paren.rparen",
            regex : "[\\)]"
        }, {
            token : "text",
            regex : "\\s+"
        } ]
    };
};

oop.inherits(AdaHighlightRules, TextHighlightRules);

exports.AdaHighlightRules = AdaHighlightRules;
});

define("ace/mode/gusto",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/gusto_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextMode = require("./text").Mode;
var AdaHighlightRules = require("./gusto_highlight_rules").AdaHighlightRules;

var Mode = function() {
    this.HighlightRules = AdaHighlightRules;
    this.$behaviour = this.$defaultBehaviour;
};
oop.inherits(Mode, TextMode);

(function() {

    this.lineCommentStart = "--";

    this.$id = "ace/mode/gusto";
}).call(Mode.prototype);

exports.Mode = Mode;

});

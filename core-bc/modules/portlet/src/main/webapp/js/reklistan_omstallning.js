var ReklistanOmstallning = {

    ajaxEventHandler: function(xhr, portletNamespace) {

        var xhrStatus = xhr.status;

        var portletNode = document.getElementById('p_p_id' + portletNamespace);
        var ajaxMasks = portletNode.getElementsByClassName('ajax-mask');
        var ajaxMask = ajaxMasks[0];

        switch(xhrStatus) {
            case 'begin':
                ReklistanOmstallning.addCssClass(ajaxMask, 'ajax-mask-active');
                break;
            case 'complete':
                ReklistanOmstallning.removeCssClass(ajaxMask, 'ajax-mask-active');
                break;
            case 'success':
                break;
        }
    },

    hasCssClass: function(elem, className) {
        if(elem) {
            return new RegExp(' ' + className + ' ').test(' ' + elem.className + ' ');
        }
    },

    addCssClass: function(elem, className) {
        if(elem) {
            if (!ReklistanOmstallning.hasCssClass(elem, className)) {
                elem.className += ' ' + className;
            }
        }
    },

    removeCssClass: function(elem, className) {
        var newClass = ' ' + elem.className.replace( /[\t\r\n]/g, ' ') + ' ';
        if (ReklistanOmstallning.hasCssClass(elem, className)) {
            while (newClass.indexOf(' ' + className + ' ') >= 0 ) {
                newClass = newClass.replace(' ' + className + ' ', ' ');
            }
            elem.className = newClass.replace(/^\s+|\s+$/g, '');
        }
    },

    someFunction: {}
};
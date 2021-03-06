var activeTab = 0;
var $msg;

function setActiveTab() {
    if(activeTab == 0) {
        $('.loginitem').show();
        $('.regitem').hide();
    }
    else {
        $('.loginitem').hide();
        $('.regitem').show();
    }

    setTimeout(function() {
        var $e = $('#email');
        if(!$e.val() && !client.browser.ie)
            $('#email')[0].focus();
        else
            $('#loginBtn')[0].focus();
    }, 100);
}


function switchTabs() {
    if(this.id.charAt(3) !== '' + activeTab) {
        $('.tab').toggleClass('active').toggleClass('inactive');
        activeTab = 1 - activeTab;
        setActiveTab();
    }
}


/**
 * @return {?string} - parsed email or null if invalid
 */
function validateEmail() {
    /* @type {string} */
    var email = $('#email').val();
    email = email.replace(/^ */, '').replace(/ *$/, '');
    if(!/\S+@\S+\.\S+/.test(email)) {
        message('Helytelen e-mail formátum');
        return null;
    }
    return email;
}


/**
 * @return {?string} - password or null if invalid
 */
function validatePassword() {
    /* @type {string} */
    var pwd1 = $('#password').val();
    /* @type {string} */
    var pwd2 = $('#password2').val();
    if(activeTab == 1 && pwd1 !== pwd2) {
        message('A jelszók nem egyeznek');
        return null;
    }
    if(!pwd1.length) {
        message('Hiányzik a jelszó');
        return null;
    }
    var res = /\S+.*\S+/.exec(pwd1);
    if(!res || res[0].length < 5) {
        message('A jelszó minimum 5 karakter');
        return null;
    }
    return pwd1;
}


function login() {
    var email = validateEmail();
    if(!email)
        return;
    var pwd = validatePassword();
    if(!pwd)
        return;
    if(pwd.length === 33 && pwd.charAt(0) === '@')
        pwd = pwd.substring(1);
    else
        pwd = window.md5(pwd);

    $.ajax({
        url: '/app/auth/authenticate',
        method: 'POST',
        dataType: 'json',
        data: {
            email: email,
            password: pwd
        },

        success: function(json)
        {
            if(json.message)
                message(json.message);
            else if(json.fail)
                message('Sikertelen belépés');
            else if(json.error)
                errorMsg(json.error);
            else
                post(location.href, {
                    username: email,
                    password: '@' + pwd
                });
        },

        error: function(/*xhr, status*/)
        {
            message('Hálózati hiba');
        }
    });
}


function register() {
    var email = validateEmail();
    if(!email)
        return;
    var pwd = validatePassword();
    if(!pwd)
        return;
    pwd = window.md5(pwd);

    $.ajax({
        url: '/app/auth/register',
        method: 'POST',
        dataType: 'json',
        data: {
            email: email,
            password: pwd,
            name: $('#name').val()
        },

        success: function(json)
        {
            if(json.message)
                message(json.message);
            else if(json.error)
                errorMsg(json.error);
            else
                post('/app', {
                    username: email,
                    password: '@' + pwd
                });
        },

        error: function(/*xhr, status*/)
        {
            message('Hálózati hiba');
        }
    });
}


function message(s) {
    $msg.html(s).show();
}


function errorMsg(code) {
    message('Hiba történt. <a href="mailto:dev@pandit.hu?subject=Hibajelentés ('+
        code+')">Beszámolok róla</a>');
}


function keydown(e) {
    var c = e.keyCode;
    $msg.hide();
    var tag = document.activeElement.tagName.toLowerCase();
    if(c === 13 && tag === 'input') {
        if(activeTab === 0)
            login();
        else
            register();
    }
}


$(function() {
    setActiveTab();
    $('.tab').click(switchTabs);
    $('#loginBtn').click(login);
    $('#regBtn').click(register);
    $(window).keydown(keydown);
    $msg = $('#loginMsg');
});


// http://stackoverflow.com/questions/2382329/how-can-i-get-browser-to-prompt-to-save-password
function post(path, params, method)
{
    method = method || "post"; // Set method to post by default if not specified.

    // The rest of this code assumes you are not using a library.
    // It can be made less wordy if you use one.

    var form = document.createElement("form");
    form.id = "dynamicform" + Math.random();
    form.setAttribute("method", method);
    form.setAttribute("action", path);
    form.setAttribute("style", "display: none");
    // Internet Explorer needs this
    form.setAttribute("onsubmit", "window.external.AutoCompleteSaveForm(document.getElementById('" + form.id + "'))");

    for (var key in params)
    {
        if (params.hasOwnProperty(key))
        {
            var hiddenField = document.createElement("input");
            // Internet Explorer needs a "password"-field to show the store-password-dialog
            hiddenField.setAttribute("type", key == "password" ? "password" : "text");
            hiddenField.setAttribute("name", key);
            hiddenField.setAttribute("value", params[key]);

            form.appendChild(hiddenField);
        }
    }

    var submitButton = document.createElement("input");
    submitButton.setAttribute("type", "submit");

    form.appendChild(submitButton);

    document.body.appendChild(form);

    //form.submit(); does not work on Internet Explorer
    submitButton.click(); // "click" on submit-button needed for Internet Explorer
}

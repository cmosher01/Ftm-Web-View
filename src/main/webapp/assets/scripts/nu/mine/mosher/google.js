function onSignIn(response) {
    var c = Cookies.get('idtoken');
    if (c === undefined) {
        document.cookie = "idtoken="+response.credential+" ; Path=/ ; Max-Age=900";
        window.location.reload(true);
    }
}

function signOut() {
    google.accounts.id.disableAutoSelect();
    document.cookie = "idtoken= ; Path=/ ; Expires = Thu, 01 Jan 1970 00:00:00 GMT";
    window.location.reload(true);
}

function ready() {
    document.getElementById('signout').addEventListener('click', signOut);
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', ready);
} else {
    ready();
}

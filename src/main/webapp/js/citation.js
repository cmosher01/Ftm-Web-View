function copyToClipboard(t) {
    function listener(e) {
        e.clipboardData.setData("text/plain", t);
        e.preventDefault();
    }
    document.addEventListener("copy", listener);
    document.execCommand("copy");
    document.removeEventListener("copy", listener);

    alert("Citation copied to clipboard.");
};

function doCopy() {
    var e = document.getElementById("urn:uuid:a1a126ed-46fb-4f6c-a6a0-b58698f696ba");
    var t = e.innerText;
    t = t.replace(/[\n\r]/g, "");
    copyToClipboard(t);
}

function ready() {
    var e = document.getElementById("urn:uuid:52595c4e-d8ca-45e8-af3f-396e068adc46");
    e.onclick = doCopy;
}

document.addEventListener("DOMContentLoaded", ready);

$(document).ready(function () {
    $('#logout').click(function () {
        $.ajax({
            type: 'DELETE',
            url: '/logout',
            success: function () {
                window.location.replace("/");
            }
        });
    });

    $('#keyform').submit(function () {
        var password = $('#keyformpassword').val();
        var privateKey = $('#keyformprivatekey');
        var encrypted = sjcl.encrypt(password, privateKey.val());
        privateKey.val(encrypted);
        return true; // submit form
    });

    $('#transactform').submit(function () {
        var action = $(this).attr("action");
        var data = $(this).serialize();
        $.post(action, data, function (resp) {
            // TODO better way to enter password
            var password = prompt("What is your password?", "password");
            var decrypted = sjcl.decrypt(password, JSON.stringify(resp.encryptedKey));
            var key = new sjcl.ecc.ecdsa.secretKey(sjcl.ecc.curves.c256, new sjcl.bn(decrypted));

            var payload = sjcl.codec.hex.toBits(resp.payload);
            var hash = sjcl.hash.sha256.hash(payload);
            var signature = key.sign(hash, 2);

            var r = sjcl.bitArray.bitSlice(signature, 0, 256);
            var s = sjcl.bitArray.bitSlice(signature, 256, 512);

            $.post("/sendtransaction", {
                payload: resp.payload,
                r: sjcl.codec.hex.fromBits(r),
                s: sjcl.codec.hex.fromBits(s)
            }, function() {
                window.location.replace("/"); // TODO what to do on successful transaction?
            })
        });
        return false; // don't submit form, since we already have
    });
});

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
        var password = $('#transaction-password').val();
        $.post(action, data, function (resp) {
            // TODO this feels like a hack, eventually make it nice
            var rString = "";
            var sString = "";
            console.log("resp: " + JSON.stringify(resp));
            for (var i = 0; i < resp.encryptedKeys.length; i++) {
                var decrypted = sjcl.decrypt(password, JSON.stringify(resp.encryptedKeys[i]));
                var key = new sjcl.ecc.ecdsa.secretKey(sjcl.ecc.curves.c256, new sjcl.bn(decrypted));

                var payload = sjcl.codec.hex.toBits(resp.payload);
                var hash = sjcl.hash.sha256.hash(payload);
                var signature = key.sign(hash, 10); // TODO higher paranoid parameter

                var r = sjcl.bitArray.bitSlice(signature, 0, 256);
                var s = sjcl.bitArray.bitSlice(signature, 256, 512);

                if (i > 0) {
                    rString += ",";
                    sString += ",";
                }
                rString += sjcl.codec.hex.fromBits(r);
                sString += sjcl.codec.hex.fromBits(s);
            }

            $.post("/sendtransaction", {
                payload: resp.payload,
                r: rString,
                s: sString
            }, function() {
                window.location.replace("/"); // TODO what to do on successful transaction?
            })
        });
        return false; // don't submit form, since we already have
    });
});

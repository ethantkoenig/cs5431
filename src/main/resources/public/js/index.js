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
        var passwordGroup = $('#keyform-password-group');
        var passwordInput = passwordGroup.find('input');
        var password = passwordInput.val();
        var confirmGroup = $('#keyform-confirm-group');
        var confirmInput = confirmGroup.find('input');
        if (confirmInput.val() != password || password.length == 0) {
            passwordInput.val('');
            confirmInput.val('');
            passwordGroup.addClass('has-error');
            confirmGroup.addClass('has-error');
            // TODO eventually display an actual error message
            return false; // do not submit
        }
        var privateKey = $('#keyform-privatekey');
        var encrypted = sjcl.encrypt(password, privateKey.val());
        // TODO this causes the privatekey input of the form to briefly
        // show the encrypted key
        privateKey.val(encrypted);
        return true; // submit form
    });

    $('.delete-key').click(function (e) {
        e.preventDefault();
        $.ajax({
            type: 'DELETE',
            url: '/user/keys' + '?' + $.param({
                publickey: this.dataset.publickey
            }),
            success: function () {
                window.location.replace("/balance");
            }
        });
    });

    $('#transactform').submit(function () {
        console.log("Sending Transaction");
        var action = $(this).attr("action");
        var data = $(this).serialize();
        var password = $('#transaction-password').val();
        $.post(action, data, function (resp) {
            console.log(resp);
            // TODO this feels like a hack, eventually make it nice
            var rString = "";
            var sString = "";
            console.log("resp: " + JSON.stringify(resp));
            for (var i = 0; i < resp.encryptedKeys.length; i++) {
                var decrypted = sjcl.decrypt(password, JSON.stringify(resp.encryptedKeys[i]));
                var key = new sjcl.ecc.ecdsa.secretKey(sjcl.ecc.curves.c256, new sjcl.bn(decrypted));

                var payload = sjcl.codec.hex.toBits(resp.payload);
                var hash = sjcl.hash.sha256.hash(payload);
                var signature = key.sign(hash, 10);

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
            }, function () {
                $('#status').remove();
                $("#status-message").append('<div class="row" id="status" style="padding-top: 10px;"> <div class="alert alert-success"> <strong>Sucess!</strong> Transaction sent.  </div> </div>');
                window.location.replace("/");
            })
        }).fail(function (jqXHR, textStatus, errorThrown) {
            var error = jqXHR.responseText || "Something went wrong. Please try again.";
            $('#status').remove();
            $("#status-message").append('<div class="row" id="status" style="padding-top: 10px;"> <div class="alert alert-danger"> <strong>Error!</strong> ' + error + ' </div> </div>');
        });
        return false; // don't submit form, since we already have
    });

    $('#friend-selector').multiSelect({
        selectableHeader: "<div class='custom-header text-center'>Cannot send me money</div>",
        selectionHeader: "<div class='custom-header text-center'>Can send me money</div>",
        afterSelect: function (values) {
            console.log(values[0]);
            $.post("/friend", {
                friend: values[0]
            }, function (data) {
                console.log(data)
            });
        },
        afterDeselect: function (values) {
            console.log(values);
            $.ajax({
                type: 'DELETE',
                url: '/friend' + '?' + $.param({"friend": values[0]}),
                success: function (data) {
                    console.log(data)
                }
            });
        }
    });

});

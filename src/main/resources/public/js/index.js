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
        console.log("Sending Transaction")
        var action = $(this).attr("action");
        var data = $(this).serialize();
        var password = $('#transaction-password').val();
        $.post(action, data, function (resp) {
            console.log(resp)
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
            }, function() {
                $( "#add-error" ).append('<div class="row" style="padding-top: 30px;"> <div class="col-md-4 col-md-offset-4"> <div class="alert alert-success"> <strong>Sucess!</strong> Transaction sent. </div> </div> </div>');
                window.location.replace("/");
            })
        }).fail(function(error) {
            console.log("error!!!");
            $( "#add-error" ).append('<div class="row" style="padding-top: 30px;"> <div class="col-md-4 col-md-offset-4"> <div class="alert alert-danger"> <strong>Error!</strong> Something went wrong, please try again. </div> </div> </div>');
        });;
        return false; // don't submit form, since we already have
    });

    $('#friend-selector').multiSelect({
        selectableHeader: "<div class='custom-header text-center'>Cannot send me money</div>",
        selectionHeader: "<div class='custom-header text-center'>Can send me money</div>",
        afterSelect: function (values) {
            console.log(values[0])
            $.post("/friend", {
                friend: values[0]
            }, function(data) {
                console.log(data)
            });
        },
        afterDeselect: function (values) {
            console.log(values)
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

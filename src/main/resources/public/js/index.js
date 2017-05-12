$(document).ready(function () {

    // for registration and password-reset
    function authPasswordAndConfirm($form) {
        var $passwordGroup = $form.find('.password-form-group');
        var $passwordInput = $passwordGroup.find('input');
        var $confirmGroup = $form.find('.confirm-form-group');
        var $confirmInput = $confirmGroup.find('input');

        var password = $passwordInput.val();
        if (!validPassword(password)) {
            $passwordGroup.addClass('has-error');
            $confirmGroup.addClass('has-error');
            display_alert("Password must be at least 16 characters.", "error")

            return false; // don't submit form
        }
        if ($confirmInput.val() != password) {
            $passwordGroup.addClass('has-error');
            $confirmGroup.addClass('has-error');
            display_alert("Passwords do not match", "error")

            return false; // don't submit form
        }

        $form.find('.hidden-password').val(authSecret(password));
        return true; // submit form
    }

    // for login and account unlock
    function login($form) {
        var $passwordGroup = $form.find('.password-form-group');
        var password = $passwordGroup.find('input').val();
        if (password.length == 0) {
            display_alert("Please enter your password.", "error")
            $passwordGroup.addClass('has-error');
            return false; // don't submit form
        }
        $form.find('.hidden-password').val(authSecret(password));
        return true; // submit form
    }

    $('#registerform').submit(function () {
        var $form = $(this);
        return authPasswordAndConfirm($form);
    });

    $('#loginform').submit(function () {
        var $form = $(this);
        return login($form);
    });

    $('#logout').click(function () {
        $.ajax({
            type: 'DELETE',
            url: '/logout',
            success: function () {
                window.location.replace("/");
            }
        });
    });

    $('#resetform').submit(function () {
        var $form = $(this);
        return authPasswordAndConfirm($form);
    });

    $('#unlockform').submit(function () {
        var $form = $(this);
        return login($form);
    });

    $('#changepasswordform').submit(function () {
        var $form = $(this);
        if (!login($form)) {
            return false;
        }
        var $oldPasswordGroup = $form.find('.old-password-form-group');
        var $newPasswordGroup = $form.find('.password-form-group');
        var oldPassword = $oldPasswordGroup.find('input').val();
        var newPassword = $newPasswordGroup.find('input').val();
        var oldSecret = encryptSecret(oldPassword);
        var newSecret = encryptSecret(newPassword);
        $.ajax({
            url: "/keys",
            success: function (response) {
                response.keys.forEach(function (key) {
                    var privateKey = sjcl.decrypt(oldSecret, key.encryptedPrivateKey);
                    key.encryptedPrivateKey = sjcl.encrypt(newSecret, privateKey);
                });
                var guid = $form.find(".hidden-guid").val();
                var url = "/change_password?guid=" + guid + "&password=" + authSecret(newPassword);
                $.ajax({
                    url: url,
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(response),
                    success: function () {
                        // TODO eventually redirect to logged-in user's page
                        window.location.replace("/");
                    },
                    error: function () {
                        window.location.replace("/change_password");
                    }
                });
            }
        });
        return false; // don't submit form
    });

    $('#keyform-generate').change(function () {
        var $form = $('#keyform');
        $form.find('.publickey-form-group').find('input').prop('disabled', this.checked);
        $form.find('.privatekey-form-group').find('input').prop('disabled', this.checked);
    });

    $('#keyform').submit(function () {
        var $form = $(this);
        var $publicKeyGroup = $form.find('.publickey-form-group');
        var $publicKeyInput = $publicKeyGroup.find('input');
        var $privateKeyGroup = $form.find('.privatekey-form-group');
        var $privateKeyInput = $privateKeyGroup.find('input');
        var $passwordGroup = $form.find('.password-form-group');
        var $passwordInput = $passwordGroup.find('input');

        var password = $passwordInput.val();
        if (password.length == 0) {
            $passwordGroup.addClass('has-error');
            display_alert("Please enter a valid password.", "error")
            return false; // do not submit
        }
        if ($('#keyform-generate').is(':checked')) {
            // generate a new key
            var privateKeyD = sjcl.bn.fromBits(sjcl.random.randomWords(8, 10));
            while (privateKeyD.greaterEquals(sjcl.ecc.curves.c256.r)) {
                privateKeyD = sjcl.bn.fromBits(sjcl.random.randomWords(8, 10));
            }
            var publicKeyPoint = sjcl.ecc.curves.c256.G.mult(privateKeyD);
            var privateKeyHex = sjcl.codec.hex.fromBits(privateKeyD.toBits());
            var publicKeyHex = sjcl.codec.hex.fromBits(publicKeyPoint.x.toBits())
                + sjcl.codec.hex.fromBits(publicKeyPoint.y.toBits());
            $privateKeyInput.val(privateKeyHex);
            $publicKeyInput.val(publicKeyHex);
        }

        if (!validHexString($publicKeyInput.val(), 128)) {
            $publicKeyGroup.addClass('has-error');
            display_alert("Please enter valid hex strings for keys.", "error");
            return false; // don't submit form
        } else if (!validHexString($privateKeyInput.val(), 64)) {
            $privateKeyGroup.addClass('has-error');
            display_alert("Please enter valid hex strings for keys.", "error");
            return false; // don't submit form
        }

        var secret = encryptSecret(password);
        var encrypted = sjcl.encrypt(secret, $privateKeyInput.val());
        $form.find('input[name="publickey"]').val($publicKeyInput.val());
        $form.find('input[name="privatekey"]').val(encrypted);
        $form.find('.hidden-password').val(authSecret(password));
        return true; // submit form
    });

    $('.delete-key').click(function (e) {
        e.preventDefault();
        $.ajax({
            type: 'DELETE',
            url: '/keys' + '?' + $.param({
                publickey: this.dataset.publickey
            }),
            success: function () {
                window.location.replace("/balance");
            }
        });
    });

    $('.delete-request').click(function (e) {
        e.preventDefault();
        $.ajax({
            type: 'DELETE',
            url: '/requests' + '?' + $.param({
                tranid: this.dataset.tranid
            }),
            success: function () {
                window.location.replace("/requests");
            }
        });
    });

    $('.transactform').submit(function () {
        var $form = $(this);
        var action = $form.attr("action");
        var data = $form.serialize();
        var password = $form.find('.transaction-password').val();
        var secret = encryptSecret(password);

        $.post(action, data, function (resp) {
            var signatures = resp.encryptedKeys.map(function (encryptedKey) {
                var decrypted = sjcl.decrypt(secret, encryptedKey);
                var key = new sjcl.ecc.ecdsa.secretKey(sjcl.ecc.curves.c256, new sjcl.bn(decrypted));

                var payload = sjcl.codec.hex.toBits(resp.payload);
                var hash = sjcl.hash.sha256.hash(payload);
                var signature = key.sign(hash, 10);

                var r = sjcl.bitArray.bitSlice(signature, 0, 256);
                var s = sjcl.bitArray.bitSlice(signature, 256, 512);
                return {
                    r: sjcl.codec.hex.fromBits(r),
                    s: sjcl.codec.hex.fromBits(s)
                };
            });

            $.ajax({
                url: '/sendtransaction',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    payload: resp.payload,
                    signatures: signatures
                }),
                success: function () {
                    display_alert("Transaction sent", "success")
                    window.location.replace("/");
                }
            });
        }).fail(function (jqXHR, textStatus, errorThrown) {
            var error = jqXHR.responseText || "Something went wrong. Please try again.";
            display_alert(error, "error")
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


function validHexString(s, length) {
    if (length != null && s.length != length) {
        return false;
    }
    var hexRegExp = /[0-9A-Fa-f]*/g;
    return hexRegExp.test(s);
}

function validPassword(password) {
    return password.length >= 16;
}

function authSecret(password) {
    var shaBitArray = sjcl.hash.sha256.hash(password + "authSalt");
    return sjcl.codec.hex.fromBits(shaBitArray);
}

function encryptSecret(password) {
    var shaBitArray = sjcl.hash.sha256.hash(password + "encryptSalt");
    return sjcl.codec.hex.fromBits(shaBitArray);
}

function display_alert(message, type) {
    $('#status').remove();
    switch (type) {
        case "warning":
            $("#status-message").append('<div class="row" id="status" style="padding-top: 10px;"> <div class="alert alert-warning"> <strong>Warning!</strong> ' + message + ' </div> </div>');
            break;
        case "success":
            $("#status-message").append('<div class="row" id="status" style="padding-top: 10px;"> <div class="alert alert-success"> <strong>Success!</strong> ' + message + ' </div> </div>');
            break;
        case "error":
            $("#status-message").append('<div class="row" id="status" style="padding-top: 10px;"> <div class="alert alert-danger"> <strong>Error!</strong> ' + message + ' </div> </div>');
            break;
        default:
            break;
    }

}



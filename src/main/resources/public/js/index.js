
$(document).ready(function() {
    $('#logout').click(function(e) {
        $.ajax({
            type: 'DELETE',
            url: '/logout',
            success: function() {
                window.location.replace("/");
            }
        });
    });
});

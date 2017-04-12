<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/recover/reset" method="post">
                <div class="form-group">
                    <input type="text" class="form-control" id="passwordInput" name="password" placeholder="New password">
                </div>
                <div class="form-group">
                    <input type="text" class="form-control" id="passwordConfirmInput" name="passwordConfirm" placeholder="Confirm new password">
                </div>
                <input type="hidden" name="guid" value="${guid}">
                <input class="btn btn-primary submit-button" type="submit" value="Reset Password">
            </form>
        </div>
    </div>
</div>
<!-- /.container -->
</@layout.master>


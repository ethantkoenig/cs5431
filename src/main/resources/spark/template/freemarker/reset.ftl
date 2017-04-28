<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/reset" method="post" id="resetform">
                <div class="form-group password-form-group">
                    <input type="password" class="form-control" placeholder="New password">
                </div>
                <div class="form-group confirm-form-group">
                    <input type="password" class="form-control" placeholder="Confirm new password">
                </div>
                <input type="hidden" class="hidden-password" name="password">
                <input type="hidden" name="guid" value="${guid}">
                <input class="btn btn-primary submit-button" type="submit" value="Reset Password">
            </form>
        </div>
    </div>
</div>
<!-- /.container -->
</@layout.master>


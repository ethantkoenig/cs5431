<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/unlock" method="post" id="unlockform">
                <div class="form-group password-form-group">
                    <input type="password" class="form-control" placeholder="Password">
                </div>
                <input type="hidden" class="hidden-password" name="password">
                <input type="hidden" name="guid" value="${guid}">
                <input class="btn btn-primary submit-button" type="submit" value="Unlock Account">
            </form>
        </div>
    </div>
</div>
<!-- /.container -->
</@layout.master>


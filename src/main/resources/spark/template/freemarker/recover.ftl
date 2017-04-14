<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row padding-top">
        <div class="col-md-4 col-md-offset-4">
            <form action="/recover" method="post">
                <div class="form-group">
                    <input type="text" class="form-control" id="emailInput" name="email" placeholder="Email">
                </div>
                <input class="btn btn-primary submit-button" type="submit" value="Send Recovery Email">
            </form>
        </div>
    </div>
    <#if success??>
        <div class="row" style="padding-top: 10px;">
            <div class="col-md-4 col-md-offset-4">
                <div class="alert alert-success">
                    <strong>Success!</strong> ${success}
                </div>
            </div>
        </div>
    </#if>
</div>
<!-- /.container -->
</@layout.master>


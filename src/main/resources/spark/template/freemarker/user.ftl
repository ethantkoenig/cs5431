<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>${username}</h1>
            <p>Public keys:</p>
            <#list hashes as hash>
                <textarea style="resize: none;" readonly>${hash}</textarea><br>
            </#list>
        </div>
    </div>

    Public key: <br>
    <textarea type="text" name="publickey" form="keyform"></textarea><br>

    Private key: <br>
    <textarea type="text" name="privatekey" form="keyform"></textarea><br>


    <form method="post" action="/user/keys" id="keyform">
        <input type="submit" value="Submit">
    </form>

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

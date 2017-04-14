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
    <textarea name="publickey" form="keyform"></textarea><br>

    Private key: <br>
    <textarea name="privatekey" form="keyform" id="keyformprivatekey"></textarea><br>

    Password: <br>
    <input type="password" id="keyformpassword"><br>


    <form method="post" action="/user/keys" id="keyform">
        <input type="submit" value="Submit">
    </form>

</div>
<!-- /.container -->
</@layout.master>

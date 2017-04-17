<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>${username}</h1>
            <#list hashes as hash>
                <textarea class="form-control" style="resize: none;" readonly>${hash}</textarea><br>
            </#list>
        </div>
    </div>

<#if loggedInUser?? && loggedInUser == username>
    <div class="row">
        <div class="col-sm-6 padding-top">

            <form method="post" action="/user/keys" id="keyform" style="padding-top: 20px;">
                <div class="form-group">
                    <input type="text" class="form-control" name="publickey" placeholder="Public Key">
                </div>
                <div class="form-group">
                    <input type="text" class="form-control" name="privatekey" id="keyformprivatekey"
                           placeholder="Private Key">
                </div>
                <div class="form-group">
                    <input type="password" class="form-control" id="keyformpassword" placeholder="Password">
                </div>
                <input class="btn btn-primary" type="submit" value="Update">
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
        <div class="col-sm-6 padding-top">
            <select id='friend-selector' multiple='multiple'>
            </select>
        </div>
    </#if>

        <#if users??>
            <#list users as user>
                <#if user != username>
                    <script>
                        $('#friend-selector').append($("<option/>", {
                            value: "${user}",
                            text: "${user}",
                        }));
                    </script>
                </#if>
            </#list>
        </#if>

        <#if friends??>
            <#list friends as friend>
                <script>
                    $('#friend-selector')
                            .append($("<option></option>")
                                    .attr("value", "${friend}")
                                    .text("${friend}")
                                    .attr('selected', 'selected'));

                </script>
            </#list>
        </#if>


    </div>

</div>
<!-- /.container -->
</@layout.master>
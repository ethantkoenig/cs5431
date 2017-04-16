<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>${username}</h1>
            <#list hashes as hash>
                <textarea style="resize: none;" readonly>${hash}</textarea><br>
            </#list>
        </div>
    </div>

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
            <select multiple="multiple" id="friend-selector" name="my-select[]">
                <option value='elem_1'>elem 1</option>
                <option value='elem_2'>elem 2</option>
                <option value='elem_3'>elem 3</option>
                <option value='elem_4'>elem 4</option>
                <option value='elem_5'>elem 5</option>
            </select>
        </div>
    </div>

    <script>
        $('#friend-selector').multiSelect({
            selectableHeader: "<div class='custom-header text-center'>Can send me money</div>",
            selectionHeader: "<div class='custom-header text-center'>Cannot send me money</div>",
        });
    </script>

</div>
<!-- /.container -->
</@layout.master>

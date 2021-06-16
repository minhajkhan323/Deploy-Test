
const search = () => {

    let query = $("#search-input").val();
    console.log(query);

    if(query == '')
    {
        $(".search-result").hide();
    }
    else
    {
        console.log(query);

        let url = `http://localhost:8080/search/${query}`;

        fetch(url).then(response=>{
            return response.json();
        }).then(data => {

            console.log(data);

            let text = `<div class='list-group'>`;

            data.forEach((contact) =>{

                text+=`<a href="/user/contact/${contact.cid}" class='list-group-item list-group-action'> ${contact.name}</a>`
            });

            text += `</div>`;

            $(".search-result").html(text);
            $(".search-result").show();
            
        });

       
    }
};


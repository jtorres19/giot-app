package com.giot.tectronix.giot.model;

import org.json.JSONObject;

public class User {

    // Propiedades
    public String name;
    public String surname;
    public String username;
    public String email;
    public String gender;
    //public Long photo;
    public int age;
    public int height;
    public int weight;

    // Completamos la información de la clase con un json
    public void fillWithJson(JSONObject json) {
        // Rellenamos info
        this.name = json.optString("nombre");
        this.surname = json.optString("apellido");
        this.username = json.optString("id_usuario");
        this.email = json.optString("email");
        this.age = json.optInt("edad");
        //this.photo = json.optLong("foto");
        this.height = json.optInt("altura");
        this.weight = json.optInt("peso");
        this.gender = json.optString("genero");
    }
}

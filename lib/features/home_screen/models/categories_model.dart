class CategoriesModel {
  String slug;
  String name;
  String url;

  CategoriesModel({
    required this.slug,
    required this.name,
    required this.url,
  });

  factory CategoriesModel.fromJson(Map<String, dynamic> json) {
    return CategoriesModel(
      slug: json["slug"],
      name: json["name"],
      url: json["url"],
    );
  }
}
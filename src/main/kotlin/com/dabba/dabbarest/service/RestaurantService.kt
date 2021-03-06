package com.dabba.dabbarest.service

import com.dabba.dabbarest.dao.RestaurantDao
import com.dabba.dabbarest.dto.DishInDto
import com.dabba.dabbarest.dto.ExtraInDto
import com.dabba.dabbarest.dto.RestaurantInDto
import com.dabba.dabbarest.dto.RestaurantOutDto
import com.dabba.dabbarest.model.Dish
import com.dabba.dabbarest.model.Extra
import com.dabba.dabbarest.model.KitchenType
import com.dabba.dabbarest.model.Restaurant
import javassist.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.*
import javax.transaction.Transactional


@Service
@Transactional
class RestaurantService(private val restaurantDao: RestaurantDao) {

    fun findByName(name: String): MutableList<RestaurantOutDto> = restaurantDao.findByName(name.toUpperCase()).map { it.toDto() }.toMutableList()
    fun getAll(): MutableList<RestaurantOutDto> {
        val restaurantList = restaurantDao.findAll()
        return restaurantList.map { it.toDto() }.toMutableList()
    }

    fun findById(id: Long): RestaurantOutDto {
        return restaurantDao.findById(id).orElseThrow { NotFoundException("Ресторан с таким id не найден") }.toDto()
    }

    fun add(restaurant: RestaurantInDto, multipartFile: MultipartFile?) = restaurantDao.save(Restaurant.fromDto(restaurant)).id
    fun addDish(dishInDto: DishInDto, multipartFile: MultipartFile?): Long? {
        val restaurant: Restaurant = restaurantDao.findById(dishInDto.restaurantId).orElseThrow { NotFoundException("Ресторан с таким id не найден") }
        restaurant.addDish(Dish.fromDto(dishInDto, uploadFileAndGetName(multipartFile)))
        return restaurantDao.save(restaurant).dishes.last().id
    }

    fun addDish(dishInDto: DishInDto): Long? {
        val byteArray = ByteArray(2); // length is bounded by 7
        Random().nextBytes(byteArray);
        val generatedString = String(byteArray, Charset.forName("UTF-8"))
        val restaurant:Restaurant= restaurantDao.findById(dishInDto.restaurantId).orElseThrow{NotFoundException("Ресторан с таким id не найден")}
        restaurant.addDish(Dish.fromDto(dishInDto, generatedString))
        return restaurantDao.save(restaurant).dishes.last().id
    }

    fun addExtra(extraInDto: ExtraInDto) {
        val dish = restaurantDao.findAll().filter { it.dishes.filter { dish -> dish.id == extraInDto.dishId }.size == 1 }[0].getDish(extraInDto.dishId)
        dish?.addExtra(Extra.fromDto(extraInDto))
        val restaurant: Restaurant = restaurantDao.findAll().filter { it.dishes.contains(dish) }[0]
        restaurantDao.save(restaurant)
    }

    fun findDishById(dishId: Long): Dish? = restaurantDao.findAll().filter { it.dishes.filter { dish -> dish.id == dishId }.size == 1 }[0].getDish(dishId)
    fun deleteDishById(id: Long) {
    }

    fun deleteRestaurant(id: Long) = restaurantDao.delete(restaurantDao.findById(id).orElseThrow { NotFoundException("Ресторан с таким id не найден") })

    fun uploadFileAndGetName(file: MultipartFile?): String? {
        if (file != null) {
            val desktop = System.getProperty("user.home")
            val name: String = "$desktop/${file.name}" + "${Math.random()}"
            try {
                val bytes: ByteArray = file.bytes
                val stream = BufferedOutputStream(FileOutputStream(File(name)))
                stream.write(bytes)
                stream.close()
                return name
            } catch (e: Exception) {
                return "Вам не удалось загрузить " + name + " => " + e.message;
            }
        } else {
            return null
        }
    }

    fun setRadius(id: Long, radius: Double) {
        val restaurant = restaurantDao.findById(id).orElseThrow { NotFoundException("Ресторан с таким id не найден") }
        restaurant.serviceRadius = radius
    }

    fun findByLink(link: String): RestaurantOutDto {
        return restaurantDao.findByLink(link).orElseThrow { NotFoundException("Ресторан с таким link не найден") }.toDto()
    }


    fun initTestDb() {
        val pjons = Restaurant(
                "Папа Джонс",
                "ул. Илона Маска, д. 9",
                KitchenType.EUROPEAN,
                "9:00",
                "23:00",
                "+7(495)123-12-34",
                null,
                "order@papajohns.ru",
                20.0,
                "",
                "papaj"
        )
        restaurantDao.save(pjons)
        val pjonsid = findByName("Папа")[0].id!!
        addDish(DishInDto(pjonsid,
                "Пицца Маргарита",
                null,
                300, 450,
                "Итальянские травы, томатный соус, моцарелла, томаты"))
        addDish(DishInDto(pjonsid,
                "Пицца Мексиканская",
                null,
                380, 550,
                "Цыпленок, томатный соус, сладкий перец, красный лук, моцарелла, острый халапеньо, томаты, соус сальса"))
        addDish(DishInDto(pjonsid,
                "Пицца Пепперони",
                null,
                350, 520,
                "Пикантная пепперони, томатный соус, моцарелла"))
        addDish(DishInDto(pjonsid,
                "Пицца Овощи и грибы",
                null,
                410, 545,
                "Итальянские травы, томатный соус, кубики брынзы, шампиньоны, сладкий перец, красный лук, моцарелла, маслины, томаты"))
        val sushi = Restaurant(
                "Вкусные суши",
                "ул. Хаяо Миядзаки, д. 5",
                KitchenType.JAPANESE,
                "9:30",
                "22:30",
                "+7(495)123-12-34",
                null,
                "order@tastysushi.ru",
                4.5,
                "",
                "sushi"
        )
        restaurantDao.save(sushi)
        val sushiId = findByName("вкусные")[0].id!!
        addDish(DishInDto(sushiId,
                "Унаги ролл",
                null,
                95, 110,
                "Рис для суси, угорь, васаби, кунжут, водоросли прессованные"))
        addDish(DishInDto(sushiId,
                "Тори унаги",
                null,
                85, 95,
                "Рис для суси, фарш (куриная грудка майонез, угорь, икра сельди), васаби, водоросли прессованные"))
        addDish(DishInDto(sushiId,
                "Вакамэ сарада",
                null,
                100, 90,
                "Рис для суси, маринованные водоросли, васаби, водоросли прессованные"))
        addDish(DishInDto(sushiId,
                "Калифорния ролл",
                null,
                100, 90,
                "Рис для суси, крабовое мясо, икра летучей рыбы, майонез, огурцы, авокадо, васаби, водоросли прессованные (8 шт.)"))

        val tacoBell = Restaurant(
                "Тако Бэлл",
                "ул. Симона Боливара, д. 3",
                KitchenType.LATINO,
                "10:30",
                "22:00",
                "+7(495)123-12-34",
                null,
                "order@tacobell.ru",
                18.5,
                "",
                "tacobell"
        )
        restaurantDao.save(tacoBell)
        val tacoId = findByName("Тако")[0].id!!
        addDish(DishInDto(tacoId,
                "Чикен тако",
                null,
                195, 350,
                "Пшеничная тортилья, курица, специи (лук, чеснок, кумин, кориандр, куркума, сахар, яблоко, тамаринд), соус «Тако», соус «Чили», салат «Романо», сыр «Чеддер»"))
        addDish(DishInDto(tacoId,
                "Чикен буррито",
                null,
                195, 350,
                "Пшеничная тортилья, курица, специи (лук, чеснок, кумин, кориандр, куркума, сахар, яблоко, тамаринд), рис, соус «Тако», соус «Чили», сыр «Чеддер»"))

    }
}
import org.apache.pekko.actor.ActorSystem

object RTXTracker extends App {
  implicit val system: ActorSystem = ActorSystem("RTX-Tracker")
  implicit val twilioService: TwilioService = TwilioServiceImpl()

  private val urls: List[String] = List(
    "https://www.microcenter.com/product/690481/pny-nvidia-geforce-rtx-5080-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690266/msi-nvidia-geforce-rtx-5080-ventus-3x-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690482/pny-nvidia-geforce-rtx-5080-epic-x-rgb-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690264/msi-nvidia-geforce-rtx-5080-gaming-trio-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690238/msi-nvidia-geforce-rtx-5080-gaming-trio-white-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690235/msi-nvidia-geforce-rtx-5080-vanguard-soc-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690234/msi-nvidia-geforce-rtx-5080-vanguard-soc-launch-edition-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690435/asus-nvidia-geforce-rtx-5080-prime-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690477/gigabyte-nvidia-geforce-rtx-5080-windforce-sff-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690233/msi-nvidia-geforce-rtx-5080-suprim-soc-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690267/msi-nvidia-geforce-rtx-5080-ventus-3x-plus-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690263/zotac-nvidia-geforce-rtx-5080-gaming-solid-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/689737/asus-nvidia-geforce-rtx-5080-prime-overclocked-triple-fan-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690232/msi-nvidia-geforce-rtx-5080-suprim-liquid-soc-overclocked-liquid-cooled-16gb-gddr7-pcie-50-graphics-card",
    "https://www.microcenter.com/product/690049/asus-nvidia-geforce-rtx-5070-ti-prime-triple-fan-16gb-gddr7-pcie-50-graphics-card"
  ).flatMap { url =>
    List(s"$url?storeid=151", s"$url?storeid=025")
  }

  urls.foreach { url =>
    system.actorOf(RTXMonitor.props(url), s"rtx-monitor-${url.hashCode}")
  }
}
